"""
Module: main_data_fetch.py
Responsible for fetching and processing GitHub differences and tracking affected test cases.
"""

from data import GitHubDiffsFinder, GitHubTagAffectedTestCasesTracker
import data.utils as utils

def main():
    """
    Main entry point for fetching GitHub diffs and tracking affected test cases.
    """
    # Retrieve metadata from utility function
    software, prefix, versions, turns, tcs_strategy, uc_prefix = utils.get_software_data()

    # Commented out for now, as we are not using GitHubDiffsFinder to fetch data, once all data already fetched and
    # stored in the data folder, we can use this to fetch data again.
    # Instantiate GitHubDiffsFinder with software name, constructed prefix, and versions
    # diffs_finder = GitHubDiffsFinder(software, f'{prefix}_{software}_v', versions)
    # Download relevant files based on identified differences
    # diffs_finder.download_files()
    # Process the downloaded files to gather changes or diffs
    # diffs_finder.run()

    # Instantiate GitHubTagAffectedTestCasesTracker to monitor test cases affected by changes
    tag_affected_test_cases_tracker = GitHubTagAffectedTestCasesTracker(
        software=software,
        prefix=uc_prefix,
        test_suite_reduction_strategy=tcs_strategy
    )
    # Run the tracker to aggregate affected test cases
    tag_affected_test_cases_tracker.run()

if __name__ == '__main__':
    main()