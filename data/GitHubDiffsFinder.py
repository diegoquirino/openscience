import pandas as pd
import requests
import data.utils as utils
import os

LABELS = ['filename', 'base_tag', 'head_tag', 'base_tag_lines_txt', 'head_tag_lines_txt']


class GitHubDiffsFinder:

    def __init__(self, software, tag_basename, tag_versions,
                 conf_file=os.path.join(os.getcwd(), 'data', 'github.conf'),
                 environment_github_api_key_name='GITHUB_API_KEY'):
        self.repo_owner, self.repo_name = utils.get_github_repo_data(conf_file)
        self.software = software
        self.tag_basename = tag_basename
        self.tag_versions = tag_versions
        self.diffs = pd.DataFrame(columns=LABELS)
        owner_token = os.getenv(environment_github_api_key_name)
        self.headers = {'Authorization': f'token {owner_token}'}

    def run(self):
        for i in range(0, len(self.tag_versions) - 1):
            base_tag = self.tag_basename + self.tag_versions[i]
            head_tag = self.tag_basename + self.tag_versions[i + 1]
            # Fetch diffs
            self.diffs = self.get_file_diffs(base_tag, head_tag)
        # Write diffs to named csv files
        self.write_diffs_to_csv()

    def get_file_diffs(self, base_tag, head_tag):
        url = f'https://api.github.com/repos/{self.repo_owner}/{self.repo_name}/compare/{base_tag}...{head_tag}'
        response = requests.get(url, headers=self.headers)
        response_json = response.json()
        for file in response_json['files']:
            filename = file['filename']
            if filename.split('.')[-1].lower() in ['claret']:
                if 'patch' in file:
                    patch = file['patch']
                    base_tag_lines_txt = ''
                    head_tag_lines_txt = ''
                    block_base = None
                    block_head = None
                    for line in patch.splitlines():
                        if line.startswith('-') and (block_base is None or block_base == '-'):
                            block_base = '-'
                            base_tag_lines_txt += f'{line}\t'
                        elif line.startswith('+') and (block_head is None or block_head == '+'):
                            block_head = '+'
                            head_tag_lines_txt += f'{line}\t'
                        else:
                            if block_base is not None or block_head is not None:
                                temp_df = pd.DataFrame([{
                                    'filename': filename,
                                    'base_tag': base_tag,
                                    'head_tag': head_tag,
                                    'base_tag_lines_txt': base_tag_lines_txt,
                                    'head_tag_lines_txt': head_tag_lines_txt
                                }], index=[self.diffs.index.max() + 1 if not self.diffs.empty else 0])
                                self.diffs = pd.concat([self.diffs, temp_df])
                            base_tag_lines_txt = ''
                            head_tag_lines_txt = ''
                            block_base = None
                            block_head = None
        return self.diffs

    def write_diffs_to_csv(self):
        self.diffs.to_csv(os.path.join(os.getcwd(), 'data', f'{self.software}_diffs_original.csv'),
                          encoding='utf-8', index=True, index_label='index')
