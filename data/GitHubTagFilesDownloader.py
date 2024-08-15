import data.utils as utils
import os
import zipfile
from io import BytesIO
import requests


class GitHubTagFilesDownloader:

    def __init__(self, save_dir,
                 conf_file=os.path.join(os.getcwd(), 'data', 'github.conf'),
                 environment_github_api_key_name='GITHUB_API_KEY'):
        self.repo_owner, self.repo_name = utils.get_github_repo_data(conf_file)
        self.save_dir = save_dir
        owner_token = os.getenv(environment_github_api_key_name)
        self.headers = {'Authorization': f'token {owner_token}'}

    def run(self):
        # Fetch all tags
        tags = self.fetch_tags()
        # Download and extract each tag
        for tag in tags:
            tag_name = tag['name']
            self.download_and_extract_tag(tag_name)

    # Function to fetch tags from GitHub repository
    def fetch_tags(self):
        url = f"https://api.github.com/repos/{self.repo_owner}/{self.repo_name}/tags"
        response = requests.get(url, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            print(f"Failed to fetch tags: {response.status_code}")
            return []

    # Function to download and extract zip for a specific tag
    def download_and_extract_tag(self, tag):
        url = f"https://github.com/{self.repo_owner}/{self.repo_name}/archive/refs/tags/{tag}.zip"
        response = requests.get(url, headers=self.headers)

        if response.status_code == 200:
            with zipfile.ZipFile(BytesIO(response.content)) as z:
                tag_dir = os.path.join(self.save_dir, tag)
                os.makedirs(tag_dir, exist_ok=True)
                z.extractall(tag_dir)
            print(f"Extracted tag {tag} to directory: {tag_dir}")
        else:
            print(f"Failed to download tag {tag}: {response.status_code}")
