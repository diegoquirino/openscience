import pandas as pd
import requests
import data.utils as utils
import time
import os
import re


class GitHubTagAffectedTestCasesTracker:

    def __init__(self, software='rgp-diarias', test_suite_reduction_strategy='Complete', ucPrefixPattern=r'UC\d+',
                 claret_output_path='output/xlsx', conf_file=os.path.join(os.getcwd(), 'data', 'github.conf'),
                 environment_github_api_key_name='GITHUB_API_KEY'):
        self.ucPrefixPattern = ucPrefixPattern
        self.repo_owner, self.repo_name = utils.get_github_repo_data(conf_file)
        self.test_suite_reduction_strategy = test_suite_reduction_strategy
        # ATP - Reduced (Adaptive Random Testing by Jaccard Distance)
        # GT  - Reduced (Greedy Heuristic - Transition Coverage)
        # GTP - Reduced (Greedy Heuristic - Transition Pair Coverage)
        self.software = software
        self.software_original_diffs_path = os.path.join(os.getcwd(),
                                                         'data',
                                                         f'{software}_diffs_original.csv')
        self.claret_output_path = claret_output_path
        owner_token = os.getenv(environment_github_api_key_name)
        self.headers = {'Authorization': f'token {owner_token}'}

    def run(self):
        df = pd.read_csv(self.software_original_diffs_path)
        df['edit_classification_truth'] = 'HIGH/LOW'
        df['new_ct'] = 0
        df['obsolete_ct'] = 0
        df['reusable_ct'] = 0
        df['affected_cts'] = None
        df['total_ct'] = 0
        for index, row in df.iterrows():
            ucx_filename = utils.extract_ucx_from_filename(row['filename'], self.ucPrefixPattern)
            base_tag = row['base_tag']
            head_tag = row['head_tag']
            base_tag_text = utils.extract_quoted_excerpts(row['base_tag_lines_txt'])
            head_tag_text = utils.extract_quoted_excerpts(row['head_tag_lines_txt'])
            keywords = [ucx_filename, self.test_suite_reduction_strategy]
            total_test_cases = 0
            tcs_with_excerpt = set()
            print(f'{ucx_filename} - {base_tag} >> {head_tag}\n{base_tag_text}\n{head_tag_text}')
            if not base_tag_text and not head_tag_text:
                print('Ignore')
                local_file_path = 'no_file.xlsx'
            elif not base_tag_text:
                # New CT - count how many test cases were created in head_tag
                print('New TC - created in head_tag')
                local_file_path = self.download_file_from_github(head_tag, keywords)
                print(local_file_path)
                tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                    local_file_path, head_tag_text)
                df.at[index, 'total_ct'] = total_test_cases
                df.at[index, 'affected_cts'] = str(tcs_with_excerpt)
                print(f'{tcs_with_excerpt} of total {total_test_cases}\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n')
                df.at[index, 'new_ct'] = len(tcs_with_excerpt)
                total_reusable = total_test_cases - len(tcs_with_excerpt)
                df.at[index, 'reusable_ct'] = 0 if total_reusable <= 0 else total_reusable
                df.at[index, 'edit_classification_truth'] = 'HIGH'
            else:
                if not head_tag_text:
                    df.at[index, 'edit_classification_truth'] = 'HIGH'
                # Obsolete CT - count how many test cases were deleted in base_tag
                # Obsolete CT - count how many test cases were updated in head or base_tag
                print('Obsolete TC - deleted and/or updated (affected in base_tag)')
                try:
                    found_on_head_tag = False
                    local_file_path = self.download_file_from_github(base_tag, keywords)
                except Exception as error:
                    print("An exception occurred:", error)
                    found_on_head_tag = True
                    local_file_path = self.download_file_from_github(head_tag, keywords)
                print(local_file_path)
                if found_on_head_tag:
                    tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                        local_file_path, head_tag_text)
                else:
                    tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                        local_file_path, base_tag_text)
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
                print(f'An error occurred while deleting the file {local_file_path}: {e}')
            time.sleep(10)
        df = df.dropna(subset=['affected_cts'])  # Deleting None
        # Resetting the index
        if 'index' in df.columns:
            df.drop(columns=['index'], inplace=True)
            df.reset_index(drop=True, inplace=True)
        # Generating results file
        results_file_path = os.path.join(os.getcwd(), 'data', f'{self.software}_diffs_counted_'
                                                      f'{self.test_suite_reduction_strategy}.csv')
        df.to_csv(results_file_path, index=True, index_label='index')

    def download_file_from_github(self, tag, keys):
        """
        Downloads a file from a GitHub repository based on a specific tag and keywords in the file name.
        Parameters:
        - tag (str): The specific tag in the repository.
        - keywords (list): List of keywords that must be in the file name.
        Returns:
        - str: Local path of the downloaded file.
        """
        base_url = f'https://api.github.com/repos/{self.repo_owner}/{self.repo_name}/git/trees/{tag}?recursive=1'
        # Make the request to get the directory tree
        response = requests.get(base_url, headers=self.headers)
        if response.status_code != 200:
            raise Exception(f'Error accessing the GitHub API: {response.status_code}')
        tree = response.json().get('tree', [])
        # Search for the file in the specified directory
        for item in tree:
            if item['type'] == 'blob' and self.claret_output_path in item['path']:
                file_name = item['path'].split('/')[-1]
                # print(f'{tag}/{keys}/{base_url} in {file_name} >> {response.status_code} >> {len(tree)}')
                # time.sleep(10)
                if all(keyword in file_name for keyword in keys):
                    file_url = (f'https://raw.githubusercontent.com/'
                                f'{self.repo_owner}/{self.repo_name}/{tag}/{item['path']}')
                    # Download the file
                    file_response = requests.get(file_url, headers=self.headers)
                    print(f'{file_name} - {file_response.status_code} >> {file_url}')
                    if file_response.status_code == 200:
                        with open(file_name, 'wb') as f:
                            f.write(file_response.content)
                        return file_name
                    else:
                        raise Exception(f'Error downloading the file: {file_response.status_code}')
        raise Exception('File not found with the specified criteria.')

    def find_test_cases_with_excerpts_into_dowloaded_file(self, filepath, excerpts):
        """
        Identifies test cases within an Excel file that contain specific excerpts.

        This function reads an Excel file, iterates through its rows, and checks if any of the provided excerpts are present within the test case descriptions.
        It keeps track of the current test case ID and the total number of test cases in the file.

        Args:
            filepath (str): The path to the Excel file.
            excerpts (list of str): A list of excerpts to search for within the test cases.

        Returns:
            tuple: A tuple containing two elements:
                - xlsx_tcs_with_excerpt (set): A set of test case IDs that contain any of the provided excerpts.
                - xlsx_total_test_cases (int): The total number of test cases found in the Excel file.
        """
        xlsx_df = pd.read_excel(filepath).map(str)
        # xlsx_total_test_cases = 0
        # current_test_case_id = None
        # xlsx_tcs_with_excerpt = set()
        for excerpt in excerpts:
            for i, xlsx_row in xlsx_df.iterrows():
                if i == 0:
                    xlsx_total_test_cases = 0
                    current_test_case_id = None
                    xlsx_tcs_with_excerpt = set()
                if 'TC' in xlsx_row.iloc[1]:
                    current_test_case_id = xlsx_row.iloc[1]
                elif 'Size: ' in xlsx_row.iloc[3]:
                    pattern = r'Size: (\d+) test case'
                    result = re.search(pattern, xlsx_row.iloc[3])
                    if result:
                        xlsx_total_test_cases = int(result.group(1))
                else:
                    for j in range(4):
                        if utils.normalize_string(excerpt) in utils.normalize_string(xlsx_row.iloc[j]):
                            if current_test_case_id is not None:
                                xlsx_tcs_with_excerpt.add(current_test_case_id)
        return xlsx_tcs_with_excerpt, xlsx_total_test_cases
