import requests
import os
import zipfile
from io import BytesIO


# Function to fetch tags from GitHub repository
def fetch_tags(repo_owner, repo_name):
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/tags"
    response = requests.get(url)

    if response.status_code == 200:
        return response.json()
    else:
        print(f"Failed to fetch tags: {response.status_code}")
        return []


# Function to download and extract zip for a specific tag
def download_and_extract_tag(repo_owner, repo_name, tag, save_dir):
    url = f"https://github.com/{repo_owner}/{repo_name}/archive/refs/tags/{tag}.zip"
    response = requests.get(url)

    if response.status_code == 200:
        with zipfile.ZipFile(BytesIO(response.content)) as z:
            tag_dir = os.path.join(save_dir, tag)
            os.makedirs(tag_dir, exist_ok=True)
            z.extractall(tag_dir)
        print(f"Extracted tag {tag} to directory: {tag_dir}")
    else:
        print(f"Failed to download tag {tag}: {response.status_code}")


# Main function
def main():
    repo_owner = "diegoquirino"
    repo_name = "openscience"
    save_dir = "tags_directory"

    # Fetch all tags
    tags = fetch_tags(repo_owner, repo_name)

    # Download and extract each tag
    for tag in tags:
        tag_name = tag['name']
        download_and_extract_tag(repo_owner, repo_name, tag_name, save_dir)


if __name__ == "__main__":
    main()
