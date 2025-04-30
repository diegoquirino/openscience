import os
import re
import json
import logging
import configparser
from datetime import datetime
from typing import List, Tuple, Union, Set
import pandas as pd
from langchain_community.document_loaders import TextLoader
from pathlib import Path


def get_formatted_current_datetime() -> str:
    """
    Returns the current date and time formatted as a string.
    """
    now = datetime.now()
    return now.strftime("%Y%m%d%H%M")

# Configure logging
logging_file_name =f'{get_formatted_current_datetime()}-prompt_rag_llms_tcm_mbt_logs.txt'
logging.basicConfig(filename=logging_file_name,
                    encoding='utf-8',
                    level=logging.INFO,
                    format='%(asctime)s %(levelname)s: %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')

def log(message: str, level: int = logging.INFO) -> None:
    """
    Logs a message to the console and a log file.

    Parameters:
    message (str): The message to log.
    level (int): The logging level.
    """
    logging.log(level, message)
    print(message)

log(f"Logging started at {logging_file_name}")

# Default JSON string for error handling
DEFAULT_JSON_STR = '''{
    "editClassification": "ERROR",
    "decisionRationale": "[error_message]"
}'''

def format_docs(docs: list) -> str:
    """
    Formats a list of documents into a single string with each document's content separated by double newlines.

    Parameters:
    docs (List): List of documents.

    Returns:
    str: Formatted string.
    """
    return "\n\n".join(doc.page_content for doc in docs)

def add_text_file_contents_to_rag_docs(path, rag_docs, glob_pattern='*'):
    """
    Add local documents to the context.

    Parameters:
    path (str): The path to the local documents.
    rag_docs (list): The list of documents to add to.
    glob_pattern (str): The pattern to match files.
    """
    for file_path in Path(path).glob(glob_pattern):
        log(f'Adding {file_path} local document...')
        # Load the document from the file path
        loader = TextLoader(str(file_path))
        # Convert each document to a JSON formatted string
        json_docs = [
            json.dumps({"metadata": doc.metadata, "page_content": doc.page_content})
            for doc in loader.load()
        ]
        # Extend rag_docs with the JSON formatted documents
        rag_docs.extend(json_docs)
    return rag_docs

def get_key(key: str, section: str = 'project_props', conf_file: str = 'app.conf') -> Union[str, None]:
    """
    Retrieves a value from a configuration file.

    Parameters:
    key (str): The key to retrieve.
    section (str): The section in the configuration file.
    conf_file (str): The path to the configuration file.

    Returns:
    Union[str, None]: The value of the key, or None if not found.
    """
    config = configparser.ConfigParser()
    config.read(conf_file)
    try:
        return config.get(section, key)
    except (configparser.NoSectionError, configparser.NoOptionError) as e:
        logging.error(f"Error reading key '{key}' from section '{section}': {e}")
        return None

def get_software_data() -> Tuple[str, str, dict, int, str, str]:
    """
    Retrieves software data from the environment and configuration file.

    Returns:
    Tuple[str, str, dict, int, str, str]: A tuple containing software data.
    """
    file = os.path.join(os.getcwd(), "data", f"sw_{os.environ['SOFTWARE']}", f"{os.environ['SOFTWARE']}.conf")
    return (
        get_key("SOFTWARE", section="software_props", conf_file=file),
        get_key("PREFIX", section="software_props", conf_file=file),
        json.loads(get_key("VERSIONS", section="software_props", conf_file=file)),
        int(get_key("ROUNDS", section="software_props", conf_file=file)),
        get_key("TCS_GENERATION_STRATEGY", section="software_props", conf_file=file),
        get_key("UC_PREFIX", section="software_props", conf_file=file)
    )

def extract_ucx_from_filename(filename: str, pattern: str = r'UC\d+') -> Union[str, None]:
    """
    Extracts a UCX identifier from a filename using a regex pattern.

    Parameters:
    filename (str): The filename to extract from.
    pattern (str): The regex pattern to use.

    Returns:
    Union[str, None]: The extracted UCX identifier, or None if not found.
    """
    matches = re.findall(pattern, str(filename))
    return matches[0] if matches else None

def extract_quoted_excerpts(text: str) -> List[str]:
    """
    Extracts quoted excerpts from a given text.

    Parameters:
    text (str): The text to extract from.

    Returns:
    List[str]: A list of quoted excerpts.
    """
    try:
        quote_pattern = r'"([^"]*?)"'
        step_pattern = r'(step.*|postCondition.*|preCondition.*)'
        matches = re.findall(step_pattern, text)
        excerpts = [quote for match in matches for quote in re.findall(quote_pattern, match)]
        if not excerpts and 'version' not in text and 'type' not in text and 'user' not in text and 'date' not in text:
            excerpts = [normalize_string(part, ' ').strip() for part in text.split('"') if part.strip()]
        return excerpts
    except Exception as e:
        logging.error(f"Error extracting quoted excerpts: {e}")
        return []

def normalize_string(s: str, repl: str = '') -> str:
    """
    Normalizes a string by removing non-alphanumeric characters and converting to lowercase.

    Parameters:
    s (str): The string to normalize.
    repl (str): The replacement string for non-alphanumeric characters.

    Returns:
    str: The normalized string.
    """
    return re.sub(r'\W+', repl, s.strip().lower())

def extract_json_answer(input_string: str) -> dict:
    """
    Extracts a JSON object from a string.

    Parameters:
    input_string (str): The string containing the JSON object.

    Returns:
    dict: The extracted JSON object.
    """
    try:
        match = re.search(r'\{.*?}', input_string, re.DOTALL)
        if match:
            json_str = match.group().replace("editClassification", "edit_classification").replace("decisionRationale", "decision_rationale")
            return json.loads(json_str)
        else:
            raise ValueError("No JSON found")
    except Exception as e:
        logging.error(f"Error extracting JSON: {e}")
        return json.loads(DEFAULT_JSON_STR.replace('[error_message]', str(e)))

def union_sets(sets: List[Set]) -> Set:
    """
    Unions a list of sets into a single set.

    Parameters:
    sets (List[Set]): The list of sets to union.

    Returns:
    Set: The union of all sets.
    """
    return set().union(*sets)

def union_set_size(row: pd.Series) -> int:
    """
    Calculates the size of the union of specific columns in a DataFrame row.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    int: The size of the union.
    """
    return len(set().union(*(row[col] for col in ['obsolete_cts_low_low', 'obsolete_cts_low_high', 'obsolete_cts_high'] if pd.notna(row[col]))))

def calculate_set_size(value: Union[set, str, None]) -> int:
    """
    Calculates the size of a set or a string representation of a set.

    Parameters:
    value (Union[set, str, None]): The set or string representation of a set.

    Returns:
    int: The size of the set.
    """
    if pd.isna(value):
        return 0
    if isinstance(value, set):
        return len(value)
    if isinstance(value, str):
        try:
            return len(eval(value))
        except:
            return 0
    return 0

def get_min_positive_difference(row: pd.Series) -> float:
    """
    Calculates the minimum positive difference between total test cases and specific columns in a DataFrame row.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    float: The minimum positive difference.
    """
    differences = [row['total_cts_size'] - row[col] for col in ['new_cts_size', 'obsolete_cts_size'] if row['total_cts_size'] - row[col] >= 0]
    return min(differences) if differences else float('nan')

def sets_of_cts_from(row: pd.Series) -> Tuple[Set, Set, Set, Set]:
    """
    Extracts sets of test cases from specific columns in a DataFrame row.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    Tuple[Set, Set, Set, Set]: A tuple of sets of test cases.
    """
    return (
        row['new_cts'] if pd.notna(row['new_cts']) else set(),
        row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set(),
        row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set(),
        row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    )

def calculate_low_impacted_size(row: pd.Series) -> int:
    """
    Calculates the size of low impacted test cases.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    int: The size of low impacted test cases.
    """
    new_cts, obsolete_cts_low_low, obsolete_cts_low_high, obsolete_cts_high = sets_of_cts_from(row)
    return len((obsolete_cts_low_low - obsolete_cts_low_high) & (obsolete_cts_low_low - obsolete_cts_high) & (obsolete_cts_low_low - new_cts))

def calculate_high_impacted_size(row: pd.Series) -> int:
    """
    Calculates the size of high impacted test cases.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    int: The size of high impacted test cases.
    """
    new_cts, obsolete_cts_low_low, obsolete_cts_low_high, obsolete_cts_high = sets_of_cts_from(row)
    return len((obsolete_cts_high - obsolete_cts_low_low) & (obsolete_cts_high - obsolete_cts_low_high) & (obsolete_cts_high - new_cts))

def calculate_mixed_impacted_size(row: pd.Series) -> int:
    """
    Calculates the size of mixed impacted test cases.

    Parameters:
    row (pd.Series): The DataFrame row.

    Returns:
    int: The size of mixed impacted test cases.
    """
    new_cts, obsolete_cts_low_low, obsolete_cts_low_high, obsolete_cts_high = sets_of_cts_from(row)
    return len(obsolete_cts_low_high.union(obsolete_cts_low_low.intersection(obsolete_cts_high.union(new_cts))))

def extract_model_and_round(path: str) -> Tuple[Union[str, None], Union[str, None]]:
    """
    Extracts the model and round from a file path.

    Parameters:
    path (str): The file path.

    Returns:
    Tuple[Union[str, None], Union[str, None]]: A tuple containing the model and round.
    """
    pattern = fr'\{os.sep}([A-Za-z0-9.-]+)-results-(\d+)\.csv$'
    match = re.search(pattern, path)
    return (match.group(1), match.group(2)) if match else (None, None)

def uppercase_columns_values(df: pd.DataFrame, columns: List[str]) -> pd.DataFrame:
    """
    Converts the values of specified columns in a DataFrame to uppercase.

    Parameters:
    df (pd.DataFrame): The DataFrame.
    columns (List[str]): The list of columns to convert.

    Returns:
    pd.DataFrame: The DataFrame with uppercase values.
    """
    for column in columns:
        if column in df.columns:
            df[column] = df[column].apply(lambda x: x.upper() if isinstance(x, str) else x)
    return df


def successive_intersections(sets: list[set]) -> set:
    """
    Performs successive intersections of sets in a list,
    starting from the largest to the smallest set.

    :param sets: List of sets (set).
    :return: The resulting set from the intersections.
    """
    # Remove empty sets
    sets = [s for s in sets if s]
    # If there are no valid sets, return an empty set
    if not sets:
        return set()
    # Sort sets by size (largest to smallest)
    sets.sort(key=len, reverse=True)
    # Initialize the intersection with the largest set
    result = sets[0]
    # Apply successive intersections
    for s in sets[1:]:
        result.intersection_update(s)

    return result

def clear(str_value, pattern=r'[^a-zA-Z0-9]', repl = ''):
    if str_value:
        if isinstance(str_value, list):
            text = ' '.join(str_value)
        elif not isinstance(str_value, str):
            text = str(str_value)
        else:
            text = str_value
        text = re.sub(r'[ \t]+', ' ', text)
        text = re.sub(r'(\r?\n)+', '\n', text)
        return re.sub(pattern, repl, text)
    return None

def csv_and_xlsx_and_view_latex(df, file_path_without_extension, iqr_column=None):
    df.to_csv(file_path_without_extension + '.csv', index=False)
    df.to_excel(file_path_without_extension + '.xlsx', index=False)
    log(df.to_latex(index=False))
    if iqr_column:
        print_iqr_lower_upper_bound(df, iqr_column)


def calculate_iqr_bounds(df, column):
    """
    Calculate the IQR bounds for the specified column.

    :param df: DataFrame containing the data.
    :param column: Column name to calculate IQR bounds for.
    :return: Tuple containing the quartiles, IQR, lower bound, and upper bound.
    """
    # Calculate the first quartile (Q1)
    Q1 = df[column].quantile(0.25)
    # Calculate the second quartile (Q2)
    Q2 = df[column].quantile(0.50)
    # Calculate the third quartile (Q3)
    Q3 = df[column].quantile(0.75)
    # Calculate the fourth quartile (Q4)
    Q4 = df[column].quantile(1.00)
    # Calculate the interquartile range (IQR)
    IQR = Q3 - Q1
    # Calculate the lower bound
    lower_bound = Q1 - 1.5 * IQR
    # Calculate the upper bound
    upper_bound = Q3 + 1.5 * IQR
    return Q1, Q2, Q3, Q4, IQR, lower_bound, upper_bound


def print_iqr_lower_upper_bound(df, column):
    """
    Print the IQR bounds for the specified column.

    :param df: DataFrame containing the data.
    :param column: Column name to calculate IQR bounds for.
    :return: Tuple containing the lower and upper bounds.
    """
    # Calculate the IQR bounds for the specified column
    Q1, Q2, Q3, Q4, IQR, lower_bound, upper_bound = calculate_iqr_bounds(df, column)
    # Print the quartiles and bounds
    log(f"IQR BY COLUMN = '{column}'")
    log(f"Q1: {Q1}")
    log(f"Q2: {Q2}")
    log(f"Q3: {Q3}")
    log(f"Q4: {Q4}")
    log(f"IQR: {IQR}")
    log(f"Lower Bound: {lower_bound}")
    log(f"Upper Bound: {upper_bound}\n")
    return lower_bound, upper_bound
