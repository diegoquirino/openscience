import pandas as pd
import os.path
import re
import csv
import requests
import json

LABELS = ['filename', 'base_tag', 'head_tag', 'base_tag_lines_txt', 'head_tag_lines_txt']


class GitHubDiffFinder:

    def __init__(self, tag_basename, tag_versions):
        self.tag_basename = tag_basename
        self.tag_versions = tag_versions
        self.diffs = pd.DataFrame(columns=LABELS)

    def get_file_diffs(self, owner, repo, base_tag, head_tag):
        url = f'https://api.github.com/repos/{owner}/{repo}/compare/{base_tag}...{head_tag}'
        response = requests.get(url)
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
                                    "filename": filename,
                                    "base_tag": base_tag,
                                    "head_tag": head_tag,
                                    "base_tag_lines_txt": base_tag_lines_txt,
                                    "head_tag_lines_txt": head_tag_lines_txt
                                }], index=[self.diffs.index.max() + 1 if not self.diffs.empty else 0])
                                self.diffs = pd.concat([self.diffs, temp_df])
                            base_tag_lines_txt = ''
                            head_tag_lines_txt = ''
                            block_base = None
                            block_head = None
        return self.diffs

    def write_diffs_to_csv(self, csv_file):
        self.diffs.to_csv(csv_file, encoding="utf-8", index=True, index_label='index')

    def run(self, owner, repo, file='diffs.csv'):
        for i in range(0, len(self.tag_versions) - 1):
            base_tag = self.tag_basename + self.tag_versions[i]
            head_tag = self.tag_basename + self.tag_versions[i + 1]
            # Fetch diffs
            self.diffs = self.get_file_diffs(owner, repo, base_tag, head_tag)
        # Write diffs to named csv files
        self.write_diffs_to_csv(file)


if __name__ == '__main__':
    # Configurations to RGP-Diarias #############
    prefix = 'exp01'
    software = 'rgp-diarias'
    versions = ['0.1', '0.2', '1.0.1', '1.0.2', '1.0.3', '1.1', '1.1.1', '1.2', '1.2.1', '1.2.3', '1.2.4', '1.2.5', '1.3']
    # Configurations to GTI-Competencias ########
    # prefix = 'exp01'
    # software = 'gti-competencias'
    # versions = ['0.1', '1.0']
    diffs_finder = GitHubDiffFinder(f'{prefix}_{software}_v', versions)
    owner = 'diegoquirino'
    repo = 'openscience'
    diffs_finder.run(owner, repo, f'{software}_diffs_original.csv')
