import pandas as pd
import requests
import os
from data.utils import clear

# Column labels for the pandas DataFrame that stores the diffs
LABELS = ['previous_filename', 'filename', 'base_tag', 'head_tag', 'base_tag_lines_txt', 'head_tag_lines_txt']


class GitHubDiffsFinder:
    '''
    A class to find file differences between consecutive GitHub tags and save the results in a CSV file.

    Attributes:
    -----------
    repo_owner : str
        The owner of the GitHub repository, retrieved from the environment variable 'GIT_REPO_OWNER'.
    repo_name : str
        The name of the GitHub repository, retrieved from the environment variable 'GIT_REPO_NAME'.
    software : str
        The name of the software or repository, used for naming the output CSV file.
    tag_basename : str
        The base string used to form complete GitHub tag names.
    tag_versions : list
        A list of version numbers to append to the base tag name for creating full tag names.
    diffs : DataFrame
        A pandas DataFrame used to store the file diffs between tags.
    headers : dict
        The headers used for making API requests to GitHub, including the Authorization token.

    Methods:
    --------
    run():
        Iterates over consecutive tag versions, fetches diffs, and writes them to a CSV file.
    get_file_diffs(base_tag, head_tag):
        Fetches and processes the differences between two GitHub tags using the GitHub API.
    write_diffs_to_csv():
        Saves the diffs DataFrame to a CSV file in the appropriate directory.
    def download_files():
        Downloads specific files (*.claret and *.xlsx) from a GitHub repository for multiple tag versions.
    '''

    def __init__(self, software, tag_basename, tag_versions):
        '''
        Initializes the GitHubDiffsFinder with repository details and tag information.

        Parameters:
        -----------
        software : str
            The name of the software, used for naming the CSV file.
        tag_basename : str
            The base string for forming tag names (e.g., 'exp0_sw1_v' for tags like 'exp0_sw1_v1.0').
        tag_versions : list
            A list of version numbers to append to the base tag name.
        '''
        # Fetching repository details from environment variables
        self.repo_owner = os.environ['GIT_REPO_OWNER']
        self.repo_name = os.environ['GIT_REPO_NAME']
        self.software = software
        self.tag_basename = tag_basename
        self.tag_versions = tag_versions
        self.diffs = pd.DataFrame(columns=LABELS)  # Initialize the DataFrame for storing diffs
        self.headers = {'Authorization': f"token {os.environ['GITHUB_API_KEY']}"}  # GitHub API headers

    def run(self):
        '''
        Iterates through the tag versions, comparing consecutive tags, and saves the diffs to a CSV file.
        '''
        # Loop over tag_versions to compare consecutive versions
        for i in range(0, len(self.tag_versions) - 1):
            base_tag = self.tag_basename + self.tag_versions[i]  # Construct the base tag name
            head_tag = self.tag_basename + self.tag_versions[i + 1]  # Construct the head tag name

            # Fetch file diffs between base_tag and head_tag
            self.diffs = self.get_file_diffs(base_tag, head_tag)

        # Write the collected diffs to a CSV file
        self.write_diffs_to_csv()

    def get_file_diffs(self, base_tag, head_tag):
        '''
        Fetches the diffs between two GitHub tags and processes changes in .claret files.

        Parameters:
        -----------
        base_tag : str
            The base tag for comparison.
        head_tag : str
            The head tag for comparison.

        Returns:
        --------
        diffs : DataFrame
            A pandas DataFrame containing the diffs between the tags, with specific lines added or removed.
        '''
        # Construct the GitHub API URL for comparing the two tags
        url = f'https://api.github.com/repos/{self.repo_owner}/{self.repo_name}/compare/{base_tag}...{head_tag}'
        response = requests.get(url, headers=self.headers)  # Send the API request
        response_json = response.json()  # Parse the JSON response

        # Iterate over the files in the response
        for file in response_json['files']:
            filename = file['filename']
            try:
                previous_filename = file['previous_filename']
            except Exception:
                previous_filename = None
            # Only process files with the .claret extension
            if filename.split('.')[-1].lower() in ['claret']:
                if 'patch' in file:
                    patch = file['patch']
                    base_tag_lines_txt = ''
                    head_tag_lines_txt = ''
                    block_base = None
                    block_head = None

                    # Iterate through each line of the patch (diff)
                    for line in patch.splitlines():
                        if line.startswith('-') and (block_base is None or block_base == '-'):
                            block_base = '-'
                            base_tag_lines_txt += f'{line}\n'  # Capture removed lines
                        elif line.startswith('+') and (block_head is None or block_head == '+'):
                            block_head = '+'
                            head_tag_lines_txt += f'{line}\n'  # Capture added lines
                        else:
                            # Once a block of diffs is processed, store it in the DataFrame
                            if clear(block_base) is not None or clear(block_head) is not None:
                                temp_df = pd.DataFrame([{
                                    'filename': filename,
                                    'previous_filename': previous_filename,
                                    'base_tag': base_tag,
                                    'head_tag': head_tag,
                                    'base_tag_lines_txt': base_tag_lines_txt,
                                    'head_tag_lines_txt': head_tag_lines_txt
                                }], index=[self.diffs.index.max() + 1 if not self.diffs.empty else 0])
                                self.diffs = pd.concat([self.diffs, temp_df])  # Append to the DataFrame

                            # Reset variables for the next diff block
                            base_tag_lines_txt = ''
                            head_tag_lines_txt = ''
                            block_base = None
                            block_head = None
        return self.diffs

    def write_diffs_to_csv(self):
        '''
        Writes the DataFrame containing diffs to a CSV file named according to the software being processed.
        '''
        # Define the CSV file path using the software name and ensure the data directory exists
        csv_file_path = os.path.join(os.getcwd(), 'data', f'sw_{self.software}', f'{self.software}_diffs_original.csv')
        os.makedirs(os.path.dirname(csv_file_path), exist_ok=True)  # Ensure the directory exists

        # Write the diffs DataFrame to a CSV file
        self.diffs.to_csv(csv_file_path, encoding='utf-8', index=True, index_label='index')

    def download_files(self):
        '''
            Downloads specific files (*.claret and *.xlsx) from a GitHub repository for multiple tag versions.

            This method connects to the GitHub API to fetch files with specified extensions
            from defined paths within a repository. It iterates over a list of tag versions,
            downloading relevant files for each version, and saves them into organized local
            directories based on the version tags.
        '''
        base_url = f'https://api.github.com/repos/{self.repo_owner}/{self.repo_name}/contents'
        local_dir = os.path.join(os.getcwd(), 'data', f'sw_{self.software}', 'repo_copy')
        os.makedirs(local_dir, exist_ok=True)
        # Define file paths to download
        paths_to_download = ['src', 'output/xlsx']
        extensions = ['.claret', '.xlsx']
        for version in self.tag_versions:
            for path in paths_to_download:
                response = requests.get(f'{base_url}/{path}?ref={self.tag_basename}{version}', headers=self.headers)
                response.raise_for_status()
                contents = response.json()
                for item in contents:
                    if item['type'] == 'file':
                        # Check if the file has a relevant extension
                        if any(item['name'].endswith(ext) for ext in extensions):
                            file_url = item['download_url']
                            version_dir = os.path.join(local_dir, f'{self.tag_basename}{version}')
                            os.makedirs(version_dir, exist_ok=True)
                            file_path = os.path.join(version_dir, item['name'])

                            # Download and save the file
                            print(f'Downloading {file_url} to {file_path}')
                            file_response = requests.get(file_url)
                            file_response.raise_for_status()

                            with open(file_path, 'wb') as file:
                                file.write(file_response.content)
