import pandas as pd
import numpy as np
import requests
import time
import os
import re


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


def find_test_cases_with_excerpts(filename, excerpts):
    xlsx_df = pd.read_excel(filename).map(str)
    xlsx_total_test_cases = 0
    current_test_case_id = None
    xlsx_tcs_with_excerpt = set()
    for excerpt in excerpts:
        for i, xlsx_row in xlsx_df.iterrows():
            if 'TC' in xlsx_row.iloc[1]:
                current_test_case_id = xlsx_row.iloc[1]
            elif 'Size: ' in xlsx_row.iloc[3]:
                pattern = r'Size: (\d+) test case'
                result = re.search(pattern, xlsx_row.iloc[3])
                if result:
                    xlsx_total_test_cases = int(result.group(1))
            else:
                for j in range(4):
                    if normalize_string(excerpt) in normalize_string(xlsx_row.iloc[j]):
                        if current_test_case_id is not None:
                            xlsx_tcs_with_excerpt.add(current_test_case_id)
    return xlsx_tcs_with_excerpt, xlsx_total_test_cases


def normalize_string(s, repl=''):
    s = s.strip()
    s = s.lower()
    return re.sub(r'\W+', repl, s)


def download_file_from_github(repo_owner, repository, tag, directory, keys, owner_token):
    """
    Downloads a file from a GitHub repository based on a specific tag and keywords in the file name.
    Parameters:
    - owner (str): The owner of the repository.
    - repo (str): The name of the repository.
    - tag (str): The specific tag in the repository.
    - dir_path (str): The directory in the repository to look for the file.
    - keywords (list): List of keywords that must be in the file name.
    Returns:
    - str: Local path of the downloaded file.
    """
    headers = {'Authorization': f'token {owner_token}'}
    base_url = f"https://api.github.com/repos/{repo_owner}/{repository}/git/trees/{tag}?recursive=1"
    # Make the request to get the directory tree
    response = requests.get(base_url, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Error accessing the GitHub API: {response.status_code}")
    tree = response.json().get('tree', [])
    # Search for the file in the specified directory
    for item in tree:
        if item['type'] == 'blob' and directory in item['path']:
            file_name = item['path'].split('/')[-1]
            if all(keyword in file_name for keyword in keys):
                file_url = f"https://raw.githubusercontent.com/{repo_owner}/{repository}/{tag}/{item['path']}"
                # Download the file
                file_response = requests.get(file_url, headers=headers)
                if file_response.status_code == 200:
                    with open(file_name, 'wb') as f:
                        f.write(file_response.content)
                    return file_name
                else:
                    raise Exception(f"Error downloading the file: {file_response.status_code}")
    raise Exception("File not found with the specified criteria.")


TEST_SUITE_REDUCTION_STRATEGY = 'Complete'
# GTP - Reduced (Greedy Heuristic - Transition Pair Coverage)
# GTP - Reduced (Greedy Heuristic - Transition Pair Coverage)
# GTP - Reduced (Greedy Heuristic - Transition Pair Coverage)


if __name__ == '__main__':
    token = os.getenv('GITHUB_API_KEY')
    system_under_extraction = 'rgp-diarias'
    owner = 'diegoquirino'
    repo = 'openscience'
    dir_path = 'output/xlsx'
    file_path = os.path.join(os.getcwd(), f'{system_under_extraction}_diffs_original.csv')
    df = pd.read_csv(file_path)
    df['edit_classification_truth'] = 'HIGH/LOW'
    df['new_ct'] = 0
    df['obsolete_ct'] = 0
    df['reusable_ct'] = 0
    df['affected_cts'] = None
    df['total_ct'] = 0
    for index, row in df.iterrows():
        ucx_filename = extract_ucx_from_filename(row['filename'])
        base_tag = row['base_tag']
        head_tag = row['head_tag']
        base_tag_text = extract_quoted_excerpts(row['base_tag_lines_txt'])
        head_tag_text = extract_quoted_excerpts(row['head_tag_lines_txt'])
        keywords = [ucx_filename, TEST_SUITE_REDUCTION_STRATEGY]
        total_test_cases = 0
        tcs_with_excerpt = set()
        print(f'{ucx_filename} - {base_tag} >> {head_tag}\n{base_tag_text}\n{head_tag_text}')
        if not base_tag_text and not head_tag_text:
            print('Ignore')
            local_file_path = 'no_file.xlsx'
        elif not base_tag_text:
            # New CT - count how many test cases were created in head_tag
            print('New TC - created in head_tag')
            local_file_path = download_file_from_github(owner, repo, head_tag, dir_path, keywords, token)
            print(local_file_path)
            tcs_with_excerpt, total_test_cases = find_test_cases_with_excerpts(local_file_path, head_tag_text)
            df.at[index, 'total_ct'] = total_test_cases
            df.at[index, 'affected_cts'] = str(tcs_with_excerpt)
            print(f'{tcs_with_excerpt} of total {total_test_cases}')
            df.at[index, 'new_ct'] = len(tcs_with_excerpt)
            total_reusable = total_test_cases - len(tcs_with_excerpt)
            df.at[index, 'reusable_ct'] = 0 if total_reusable <= 0 else total_reusable
            df.at[index, 'edit_classification_truth'] = 'HIGH'
        else:
            if not head_tag_text:
                df.at[index, 'edit_classification_truth'] = 'HIGH'
            # Obsolete CT - count how many test cases were deleted in base_tag
            # Obsolete CT - count how many test cases were updated in base_tag
            print('Obsolete TC - deleted and/or updated (affected in base_tag)')
            local_file_path = download_file_from_github(owner, repo, base_tag, dir_path, keywords, token)
            print(local_file_path)
            tcs_with_excerpt, total_test_cases = find_test_cases_with_excerpts(local_file_path, base_tag_text)
            df.at[index, 'total_ct'] = total_test_cases
            df.at[index, 'affected_cts'] = str(tcs_with_excerpt)
            print(f'{tcs_with_excerpt} of total {total_test_cases}')
            df.at[index, 'obsolete_ct'] = len(tcs_with_excerpt)
            total_reusable = total_test_cases - len(tcs_with_excerpt)
            df.at[index, 'reusable_ct'] = 0 if total_reusable <= 0 else total_reusable
        if int(df.at[index, 'new_ct']) == 0 and int(df.at[index, 'obsolete_ct']) == 0:
            df.at[index, 'reusable_ct'] = 0
            df.at[index, 'affected_cts'] = None
        try:
            os.remove(local_file_path)
        except Exception as e:
            print(f"An error occurred while deleting the file {local_file_path}: {e}")
        time.sleep(10)
    df = df.dropna(subset=['affected_cts'])  # Deleting None
    # Resetting the index
    if 'index' in df.columns:
        df.drop(columns=['index'], inplace=True)
        df.reset_index(drop=True, inplace=True)
    # Generating results file
    results_file_path = os.path.join(os.getcwd(),
                                     f'{system_under_extraction}_diffs_counted_{TEST_SUITE_REDUCTION_STRATEGY}.csv')
    df.to_csv(results_file_path, index=True, index_label='index')
