from datetime import datetime
import pandas as pd
import logging
import json
import re
import os


logging.basicConfig(filename='error_log.txt',
                    encoding='utf-8',
                    level=logging.ERROR,
                    format='%(asctime)s %(levelname)s: %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')


def get_formatted_current_datetime():
    now = datetime.now()
    now_formatted_datetime = now.strftime("%Y%m%d%H%M")
    return now_formatted_datetime


def get_conf_as_json(conf_file):
    config = {}
    with open(conf_file, 'r', encoding='utf-8') as f:
        for line in f:
            key, value = line.split('=')
            try:
                config[key] = eval(value)
            except (NameError, SyntaxError):
                config[key] = value.strip("'")
    return config


def get_software_data(filepath):
    config = get_conf_as_json(filepath)
    return config['PREFIX'], config['SOFTWARE'], config['VERSIONS'], config['ROUNDS'], config['TCS_GENERATION_STRATEGY']


def get_github_repo_data(filepath):
    config = get_conf_as_json(filepath)
    return config['OWNER'], config['NAME']


def log_error(message=""):
    logging.error(message)
    print(message)


def extract_json_answer(input_string):
    # Using a regular expression to extract the JSON from the string
    match = re.search(r'\{.*?}', input_string, re.DOTALL)
    if match:
        json_str = match.group()
        try:
            json_data = json.loads(json_str)
            return json_data
        except json.JSONDecodeError as e:
            log_error(f"Erro ao decodificar JSON: {e}")
            return None
    else:
        log_error("Nenhum JSON encontrado na string.")
        return None


def extract_ucx_from_filename(filename):
    pattern = r'UC\d+'
    matches = re.findall(pattern, filename)
    return matches[0]


def extract_quoted_excerpts(text):
    try:
        quote_pattern = r'"([^"]*?)"'
        step_pattern = r'(step.*|postCondition.*|preCondition.*)'
        matches = re.findall(step_pattern, text)
        excerpts = []
        for match in matches:
            quotes = re.findall(quote_pattern, match)
            excerpts.extend(quotes)
        if not excerpts:
            if 'version' not in text and 'type' not in text and 'user' not in text and 'date' not in text:
                split_texts = text.split('\"')
                if len(split_texts) > 0:
                    for a_text in split_texts:
                        a_text = normalize_string(a_text, ' ').strip()
                        if a_text is not None and a_text is not '':
                            excerpts.append(a_text)
        return excerpts
    except Exception as e:
        return []


def normalize_string(s, repl=''):
    s = s.strip()
    s = s.lower()
    return re.sub(r'\W+', repl, s)


def union_sets(sets):
    result = set()
    for s in sets:
        result = result.union(s)
    return result


def union_set_size(row):
    union_set = set()
    for col in ['obsolete_cts_low_low', 'obsolete_cts_low_high', 'obsolete_cts_high']:
        if pd.notna(row[col]):
            union_set.update(row[col])
    return len(union_set)


def calculate_set_size(value):
    if pd.isna(value):
        return 0
    elif isinstance(value, set):
        return len(value)
    elif isinstance(value, str):
        try:
            value_set = eval(value)
            return len(value_set)
        except:
            return 0
    else:
        return 0


def get_min_positive_difference(row):
    differences = []
    for col in ['new_cts_size', 'obsolete_cts_size']:
        diff = row['total_cts_size'] - row[col]
        if diff > 0:
            differences.append(diff)
        elif diff == 0:
            differences.append(0)
    return min(differences) if differences else float('nan')


def calculate_low_impacted_size(row):
    # test cases that include updated steps classified by our strategy as 'low impact'
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    return len((obsolete_cts_low_low - obsolete_cts_low_high)
               & (obsolete_cts_low_low - obsolete_cts_high)
               & (obsolete_cts_low_low - new_cts))


def calculate_high_impacted_size(row):
    # test cases that include 'high impact' steps
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    high_united = obsolete_cts_high.union(new_cts)
    return len((high_united - obsolete_cts_low_low)
               & (high_united - obsolete_cts_low_high))


def calculate_mixed_impacted_size(row):
    # test cases that include at least one 'high impact' step and at least one 'low impact' step
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    low_united = obsolete_cts_low_low.union(obsolete_cts_low_high)
    high_united = obsolete_cts_high.union(new_cts)
    return len(low_united.intersection(high_united))


def extract_model_and_round(path):
    """
    Extracts the model name and round number from a given file path.

    Args:
        path (str): The file path to extract information from.
                    It's assumed to follow the pattern: .../<model>-results-<round>.csv

    Returns:
        tuple: A tuple containing the model name and round number as strings.
               If the pattern is not found in the path, returns (None, None).
    """
    # Regular expression pattern to match model and round
    pattern = fr'\{os.sep}([a-z0-9.-]+)-results-(\d+)\.csv$'
    # Search for the pattern in the path
    match = re.search(pattern, path)
    if match:
        model = match.group(1)
        round_num = match.group(2)
        return model, round_num
    else:
        return None, None
