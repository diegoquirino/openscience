import os.path
import pandas as pd
import concurrent.futures
from strategy import ChatGPTOpenAiStrategy, GoogleGeminiStrategy, OllamaStrategy
import data.utils as utils


def process_model(model):
    for turn in range(turns):
        if 'gpt' in model:
            strategy = ChatGPTOpenAiStrategy(model)
        elif 'gemini' in model:
            strategy = GoogleGeminiStrategy(model)
        else:
            strategy = OllamaStrategy(model)
        result_df = None
        for index, row in truth_diffs_df.iterrows():
            log = f'\n[{row['index']}] {model} - turn {turn} begin :: \nOrigin: \'{row['base_tag_lines_txt']}\'\nTarget: \'{row['head_tag_lines_txt']}'
            try:
                print(f'{log}\n[{row['index']}] {model} end :: answer >>')
                result_df = strategy.classify_change(row['base_tag_lines_txt'], row['head_tag_lines_txt'])
            except Exception as e:
                message = f'{e} => {log}'
                utils.log_error(message)
        if result_df is not None:
            directory_path = f'data/use_cases_edit_classifications/{formatted_datetime}'
            os.makedirs(directory_path, exist_ok=True)
            file_path = os.path.join(directory_path, f'{model}-results-{turn}.csv')
            result_df.to_csv(file_path, index_label='index')


# MODELS = ['gpt-4o', 'gpt-4o-mini', 'gemini-1.5-flash',
#           'openchat', 'gemma2', 'llama3.1', 'qwen2', 'phi3', 'mistral', 'mixtral']
MODELS = ['mixtral', 'openchat']

if __name__ == '__main__':
    software_conf_file_path = os.path.join(os.getcwd(), 'data', 'rgp-diarias.conf')
    prefix, software, versions, turns = utils.get_software_data(software_conf_file_path)
    truth_diffs_path = os.path.join(os.getcwd(), 'data',
                                    f'{software}_diffs_cts_counted_Complete.csv')
    truth_diffs_df = pd.read_csv(truth_diffs_path)
    formatted_datetime = utils.get_formatted_current_datetime()

    with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
        executor.map(process_model, MODELS)
