import pandas as pd
import logging
import os
from data.utils import log, get_software_data, get_formatted_current_datetime
from strategy import (
    EditClassifierStrategy,
    LLMFactory,
    LevenshteinClassifier,
    NGramClassifier,
    JaccardClassifier,
    CosineClassifier,
    SorensenDiceClassifier,
    PromptCoTToTClassifier,
    NaiveRAGClassifier
)

def create_edit_classifier(llm) -> EditClassifierStrategy:
    """
    Creates and returns an edit classifier strategy based on the specified
    'strategy' and the provided LLM (if applicable).

    \param llm: The large language model (LLM) to be utilized by certain strategies.
    \return: An instance of EditClassifierStrategy for classifying text edits.
    """
    # Check if the chosen strategy is based on distance functions (DF) or other approaches
    if strategy == "DF":
        # Decide which distance-based classifier to instantiate based on 'name'
        if name == "Levenshtein":
            return LevenshteinClassifier(impact_threshold)
        elif name == "NGram":
            return NGramClassifier(impact_threshold)
        elif name == "Jaccard":
            return JaccardClassifier(impact_threshold)
        elif name == "Cosine":
            return CosineClassifier(impact_threshold)
        elif name == "SorensenDice":
            return SorensenDiceClassifier(impact_threshold)
        else:
            message = f"Unknown classifier name: {name}"
            log(message, logging.ERROR)
            raise ValueError(message)

    elif strategy == "CoTToT":
        # Use a prompt-based chain-of-thought or tree-of-thought approach
        return PromptCoTToTClassifier(
            llm=llm,
            prompt_path=os.path.join(
                os.getcwd(),
                os.getenv("COT_TOT_PROMPT_PATH")
            )
        )

    elif strategy == "NaiveRAG":
        # Use a basic Retrieval-Augmented Generation strategy
        return NaiveRAGClassifier(
            llm=llm,
            prompt_path=os.path.join(os.getcwd(), os.getenv("RAG_QUESTION_PATH")),
            context_path=os.path.join(os.getcwd(), os.getenv("RAG_CONTEXT_FILES_PATH")),
            retriever_query_path=os.path.join(os.getcwd(), os.getenv("RAG_RETRIEVER_QUERY_PATH"))
        )

    else:
        message = f"Unknown strategy: {strategy}"
        log(message, logging.ERROR)
        raise ValueError(message)

def process_edit_classification():
    """
    Iterates over data rows and classifies text edits according to the chosen
    classifier strategy. Stores results and saves them to file after each turn.
    """
    # Use an LLM-based classifier if the provider is specified, otherwise distance-based
    if provider is not None and provider != "distance_function":
        llm = LLMFactory(provider=provider, model=provider_id).get_llm()
        edit_classifier = create_edit_classifier(llm)
    else:
        llm = None
        edit_classifier = create_edit_classifier(llm)

    # Loop through each turn (as defined in the config)
    for turn in range(turns):
        # Iterate through the dataset rows containing diff information
        for index, row in truth_diffs_df.iterrows():
            origin_txt = row['base_tag_lines_txt']
            target_txt = row['head_tag_lines_txt']
            log_message = f"[{index}] {name} - {strategy} - turn {turn} begin ::"
            log(log_message)

            try:
                # Special handling for strategies involving a reference path
                if isinstance(edit_classifier, NaiveRAGClassifier):
                    origin_file = row['filename'] if pd.isna(row['previous_filename']) or row['previous_filename'] == '' \
                                  else row['previous_filename']
                    origin_file = origin_file.removeprefix('src/')
                    origin_file = os.path.join(
                        os.getcwd(),
                        'data',
                        f"sw_{software}",
                        'repo_copy',
                        row['base_tag'],
                        origin_file
                    )
                    log(f"Origin File: '{origin_file}'")
                    edit_classifier.set_origin_file_path(origin_file)

                # Perform the classification using the chosen edit classifier
                edit_classifier.classify(origin_txt, target_txt)
                log(f"Change classified for index {index} in turn {turn}")

            except Exception as e:
                # In case of errors, log and continue
                log(f"Error classifying change at index {index}: {e}", logging.ERROR)
            break

        # Save and reset the classifier's data for each turn
        if edit_classifier.get_df() is not None:
            directory_path = os.path.join(
                os.getcwd(),
                'data',
                f"sw_{software}",
                'use_cases_edit_classifications',
                f"{formatted_datetime}"
            )
            os.makedirs(directory_path, exist_ok=True)
            file_path = os.path.join(directory_path, f"{name}-{strategy}-results-{turn}.csv")
            edit_classifier.get_df().to_csv(file_path, index_label="index")
            log(f"Results saved to {file_path}")
            log("==================XXXXXXXXXXXXX==================\n\n")
        edit_classifier.reset_df()
        break

if __name__ == '__main__':
    # Load model settings from CSV, ensuring numeric columns have the correct types
    models = pd.read_csv(os.path.join(os.getcwd(), os.getenv("MODELS_PATH")))
    models['Impact Threshold'] = models['Impact Threshold'].astype(str).str.replace(",", ".").astype(float)

    # Fetch basic data from configuration
    software, prefix, versions, turns, tcs_strategy, uc_prefix = get_software_data()

    # Read the diffs count file prepared for classification
    truth_diffs_path = os.path.join(os.getcwd(), 'data', f"sw_{software}",
                                    f"{software}_diffs_counted_{tcs_strategy}.csv")
    truth_diffs_df = pd.read_csv(truth_diffs_path)
    formatted_datetime = get_formatted_current_datetime()

    # Loop through each model configuration to run the classification process
    for i, row_data in models.iterrows():
        row_dict = row_data.to_dict()
        name = row_dict.get("Name")
        provider = row_dict.get("Provider")
        provider_id = row_dict.get("Provider Id")
        impact_threshold = row_dict.get("Impact Threshold")
        strategy = row_dict.get("Global Strategy")

        # Perform the classification per model strategy
        process_edit_classification()