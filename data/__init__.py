import os
from data.utils import get_key

from .github_diffs_finder import GitHubDiffsFinder
from .github_tag_affected_test_cases_tracker import GitHubTagAffectedTestCasesTracker
from .web_scrapper import WebScraper

os.environ['GITHUB_API_KEY'] = get_key('GITHUB_API_KEY', 'api_keys')
os.environ['GIT_REPO_OWNER'] = get_key('GIT_REPO_OWNER')
os.environ['GIT_REPO_NAME'] = get_key('GIT_REPO_NAME')
os.environ['SOFTWARE'] = get_key('SOFTWARE')
