from data import GitHubDiffsFinder, GitHubTagAffectedTestCasesTracker
import data.utils as utils
import os

SOFTWARE = 'gti-competencias'
UC_PREFIX_PATTERN = r'RF\d+'

if __name__ == '__main__':
    software_conf_file_path = os.path.join(os.getcwd(), 'data', f'{SOFTWARE}.conf')
    prefix, software, versions, turns, tcs_strategy = utils.get_software_data(software_conf_file_path)
    diffs_finder = GitHubDiffsFinder(software, f'{prefix}_{software}_v', versions)
    diffs_finder.run()
    tag_affected_test_cases_tracker = GitHubTagAffectedTestCasesTracker(software, ucPrefixPattern=UC_PREFIX_PATTERN,
                                                                        test_suite_reduction_strategy=tcs_strategy)
    tag_affected_test_cases_tracker.run()
