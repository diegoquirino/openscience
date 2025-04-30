import pandas as pd
from data.utils import clear, extract_ucx_from_filename, extract_quoted_excerpts, successive_intersections
import os
import re

class GitHubTagAffectedTestCasesTracker:
    '''
    A class to track affected test cases between two GitHub tags by analyzing differences in files and test case descriptions.
    It categorizes test cases as new, obsolete, or reusable based on the content differences between tags.

    Attributes:
    -----------
    ucPrefixPattern : str
        Regular expression pattern to identify test case prefixes in filenames.
    repo_owner : str
        GitHub repository owner, retrieved from the environment variable 'GIT_REPO_OWNER'.
    repo_name : str
        GitHub repository name, retrieved from the environment variable 'GIT_REPO_NAME'.
    test_suite_reduction_strategy : str
        Strategy used for test suite reduction (e.g., 'Complete', 'ATP', 'GT', 'GTP').
    software : str
        Name of the software or repository being analyzed.
    software_original_diffs_path : str
        Path to the original diffs CSV file for the software.
    claret_output_path : str
        Path to save output files.
    headers : dict
        Headers for GitHub API requests, including the authorization token.

    Methods:
    --------
    run():
        Executes the main process of tracking affected test cases by iterating through diffs between tags.
    find_file_with_keywords(head_tag, keywords):
        Searches for the first file in the directory that contains all specified keywords.
    find_test_cases_with_excerpts_into_dowloaded_file(filepath, excerpts):
        Identifies test cases within an Excel file that contain specific excerpts.

    '''

    def __init__(self, software='rgp-diarias', prefix='UC', test_suite_reduction_strategy='Complete',
                 claret_output_path='output/xlsx'):
        '''
        Initializes the tracker with repository information and test suite settings.

        Parameters:
        -----------
        software : str
            Name of the software (default is 'rgp-diarias').
        prefix : str
            Prefix for test case IDs (default is 'UC').
        test_suite_reduction_strategy : str
            Reduction strategy for test suite (default is 'Complete').
        claret_output_path : str
            Directory path to save output files (default is 'output/xlsx').
        '''
        self.ucPrefixPattern = fr'{prefix}\d+'
        self.repo_owner = os.environ['GIT_REPO_OWNER']
        self.repo_name = os.environ['GIT_REPO_NAME']
        self.test_suite_reduction_strategy = test_suite_reduction_strategy
        self.software = software
        self.software_original_diffs_path = os.path.join(os.getcwd(),
                                                         'data', f'sw_{software}',
                                                         f'{software}_diffs_original.csv')
        self.claret_output_path = claret_output_path
        self.headers = {'Authorization': f"token {os.environ['GITHUB_API_KEY']}"}

    def run(self):
        '''
        Main execution method that processes test cases across GitHub tags to track changes.
        It updates a DataFrame to classify test cases as new, obsolete, or reusable based on tag content.

        The DataFrame is saved as a CSV file with the categorized test cases for each GitHub tag.
        '''
        df = pd.read_csv(self.software_original_diffs_path)
        df['edit_classification_truth'] = 'HIGH/LOW'
        df['new_ct'] = 0
        df['obsolete_ct'] = 0
        df['reusable_ct'] = 0
        df['affected_cts'] = None
        df['total_ct'] = 0
        for index, row in df.iterrows():
            ucx_filename = extract_ucx_from_filename(row['filename'], self.ucPrefixPattern)
            ucx_previous_filename = extract_ucx_from_filename(row['previous_filename'], self.ucPrefixPattern)
            base_tag = row['base_tag']
            head_tag = row['head_tag']
            base_tag_text = extract_quoted_excerpts(row['base_tag_lines_txt'])
            head_tag_text = extract_quoted_excerpts(row['head_tag_lines_txt'])
            keywords = [ucx_filename, self.test_suite_reduction_strategy]
            # total_test_cases = 0
            # tcs_with_excerpt = set()
            print(f'\n\n{ucx_previous_filename}/{ucx_filename} - {base_tag} >> {head_tag}\n{base_tag_text}\n{head_tag_text}')
            if not clear(base_tag_text) and not clear(head_tag_text):
                print('Ignore')
            elif not base_tag_text:
                print('New TC - created in head_tag')
                local_file_path = self.find_file_with_keywords(head_tag, keywords)
                print(local_file_path)
                tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                    local_file_path, head_tag_text)
                df.at[index, 'total_ct'] = total_test_cases
                if len(tcs_with_excerpt) == 0:
                    for i in range(1, total_test_cases + 1):
                        tcs_with_excerpt.add(f'TC{i}')
                df.at[index, 'affected_cts'] = str(tcs_with_excerpt)
                print(f'{tcs_with_excerpt} of total {total_test_cases}\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n')
                df.at[index, 'new_ct'] = len(tcs_with_excerpt)
                total_reusable = total_test_cases - len(tcs_with_excerpt)
                df.at[index, 'reusable_ct'] = 0 if total_reusable <= 0 else total_reusable
                df.at[index, 'edit_classification_truth'] = 'HIGH'
            else:
                print('Obsolete TC - deleted and/or updated (affected in base_tag)')
                try:
                    found_on_head_tag = False
                    local_file_path = self.find_file_with_keywords(base_tag, keywords)
                    if not local_file_path:
                        if ucx_previous_filename:
                            keywords = [ucx_previous_filename, self.test_suite_reduction_strategy]
                            local_file_path = self.find_file_with_keywords(base_tag, keywords)
                except Exception as error:
                    print('An exception occurred:', error)
                    found_on_head_tag = True
                    local_file_path = self.find_file_with_keywords(head_tag, keywords)
                print(local_file_path)
                if found_on_head_tag:
                    tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                        local_file_path, head_tag_text)
                else:
                    tcs_with_excerpt, total_test_cases = self.find_test_cases_with_excerpts_into_dowloaded_file(
                        local_file_path, base_tag_text)
                df.at[index, 'total_ct'] = total_test_cases
                if not head_tag_text:
                    df.at[index, 'edit_classification_truth'] = 'HIGH'
                    if len(tcs_with_excerpt) == 0:
                        for i in range(1, total_test_cases + 1):
                            tcs_with_excerpt.add(f'TC{i}')
                df.at[index, 'affected_cts'] = str(tcs_with_excerpt)
                print(f'{tcs_with_excerpt} of total {total_test_cases}')
                df.at[index, 'obsolete_ct'] = len(tcs_with_excerpt)
                total_reusable = total_test_cases - len(tcs_with_excerpt)
                df.at[index, 'reusable_ct'] = 0 if total_reusable <= 0 else total_reusable
            if int(df.at[index, 'new_ct']) == 0 and int(df.at[index, 'obsolete_ct']) == 0:
                df.at[index, 'reusable_ct'] = 0
                df.at[index, 'affected_cts'] = None
        df = df.dropna(subset=['affected_cts'])
        if 'index' in df.columns:
            df.drop(columns=['index'], inplace=True)
            df.reset_index(drop=True, inplace=True)
        results_file_path = os.path.join(os.getcwd(), 'data', f'sw_{self.software}', f'{self.software}_diffs_counted_'
                                                      f'{self.test_suite_reduction_strategy}.csv')
        df.to_csv(results_file_path, index=True, index_label='index')

    def find_file_with_keywords(self, head_tag, keywords):
        '''
        Searches for the first file in `cwd/data/sw_{self.software}/repo_copy/{head_tag}` that contains all specified keywords.

        Parameters:
        -----------
        head_tag : str
            The repository tag of interest.
        keywords : list
            List of keywords to search for within each file.

        Returns:
        --------
        str
            Path of the first file containing all keywords, or None if no matching file is found.
        '''
        search_dir = os.path.join(os.getcwd(), 'data', f'sw_{self.software}', 'repo_copy', head_tag)
        for root, _, files in os.walk(search_dir):
            for file_name in files:
                file_path = os.path.join(root, file_name)
                try:
                    if all(keyword in file_name for keyword in keywords):
                        return file_path  # Return path of the first matching file
                except Exception as e:
                    print(f'Skipping file {file_name} due to read error: {e}')
        return None

    def find_test_cases_with_excerpts_into_dowloaded_file(self, filepath, excerpts):
        '''
        Identifies test cases within an Excel file that contain specific excerpts.

        This function reads an Excel file, iterates through its rows, and checks if any of the provided excerpts are present within the test case descriptions.
        It keeps track of the current test case ID and the total number of test cases in the file.

        Parameters:
        -----------
        filepath : str
            Path to the Excel file.
        excerpts : list of str
            List of excerpts to search for within the test cases.

        Returns:
        --------
        tuple
            - A set of test case IDs that contain any of the provided excerpts.
            - The total number of test cases in the Excel file.
        '''
        xlsx_df = pd.read_excel(filepath).applymap(str)
        xlsx_total_test_cases = 0
        current_test_case_id = None
        xlsx_tcs_with_excerpt_list = []

        for excerpt in excerpts:
            xlsx_tcs_with_this_excerpt = set()
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
                        cleared_excerpt = clear(excerpt)
                        cleared_column_value = clear(xlsx_row.iloc[j])
                        if f"{cleared_excerpt}" in cleared_column_value:
                            if current_test_case_id is not None:
                                xlsx_tcs_with_this_excerpt.add(current_test_case_id)
            xlsx_tcs_with_excerpt_list.append(xlsx_tcs_with_this_excerpt)

        return successive_intersections(xlsx_tcs_with_excerpt_list), xlsx_total_test_cases

