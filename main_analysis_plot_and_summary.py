from analysis import ConfusionMatrixAnalysis, TestCasesAnalysis, SummaryAnalysis
import data.utils as utils
import pandas as pd
import os

SOFTWARE = 'rgp-diarias'
CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH = '202408132200'
DATAPATH = os.path.join(os.getcwd(), 'data', 'use_cases_edit_classifications',
                        CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH)
ANALYSIS_RESULTS_PATH = os.path.join(os.getcwd(), 'analysis', 'results',
                                     CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH)


if __name__ == '__main__':
    software_conf_file_path = os.path.join(os.getcwd(), 'data', f'{SOFTWARE}.conf')
    prefix, software, versions, turns, tcs_strategy = utils.get_software_data(software_conf_file_path)
    truth_diffs_path = os.path.join(os.getcwd(), 'data', f'{software}_diffs_cts_counted_{tcs_strategy}.csv')
    truth_df = pd.read_csv(truth_diffs_path)
    summary_analysis = SummaryAnalysis()
    for use_cases_edit_classifications_file in os.listdir(DATAPATH):
        use_case_edit_classifications_file_path = os.path.join(DATAPATH, use_cases_edit_classifications_file)
        if os.path.isfile(use_case_edit_classifications_file_path):
            file_name_path, file_extension = os.path.splitext(use_case_edit_classifications_file_path)
            file_name = file_name_path.split(os.sep)[-1]
            confusion_matrix_analysis = ConfusionMatrixAnalysis(use_case_edit_classifications_file_path,
                                                                truth_diffs_path,
                                                                classification_result='edit_classification',
                                                                classification_truth='edit_classification_truth',
                                                                conf_matrix_title=f'{file_name.upper()} Confusion Matrix',
                                                                accuracy_graph_title=f'{file_name.upper()} Accuracy')
            conf_matrix, accuracy, recall, precision, f1 = confusion_matrix_analysis.calculate()
            # confusion_matrix_analysis.plot()
            test_cases_analysis = TestCasesAnalysis(use_case_edit_classifications_file_path, truth_diffs_path)
            merged_df, grouped_df = test_cases_analysis.merge_and_process()
            os.makedirs(ANALYSIS_RESULTS_PATH, exist_ok=True)
            merged_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{file_name}-tc-merged-results.csv'), index=False)
            grouped_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{file_name}-tc-grouped-results.csv'), index=False)
            summary_analysis.add(confusion_matrix_analysis, test_cases_analysis)
    summary_df = summary_analysis.finalize()
    summary_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}-{SOFTWARE}-summary-results.csv'), index=False)
    summary_analysis.plot_metrics()
    summary_analysis.plot_lost_variance()
    summary_analysis.plot_tp_by_model_across_rounds()

    table_summary_df = summary_analysis.view_table_avarage_edit_classifications_requests()
    table_summary_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}-{SOFTWARE}-table-total-classifications-results.csv'), index=False)

    table_summary_avarage_edit_classifications_low_high_requests_df = summary_analysis.view_table_avarage_edit_classifications_low_high_requests()
    table_summary_avarage_edit_classifications_low_high_requests_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}-{SOFTWARE}-table-avarage-edit-classifications-low-high-requests-results.csv'), index=False)

    table_summary_new_reusable_obsolete_df = summary_analysis.view_table_total_new_reusable_obsolete_test_cases_per_model_and_round()
    table_summary_new_reusable_obsolete_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}-{SOFTWARE}-table-total-new-reusable-obsolete-tcs-results.csv'), index=False)

    table_summary_low_high_mixed_tcs_df = summary_analysis.view_table_low_high_mixed_test_case_impacted()
    table_summary_low_high_mixed_tcs_df.to_csv(os.path.join(ANALYSIS_RESULTS_PATH, f'{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}-{SOFTWARE}-table-total-low-high-mixed-tcs-results.csv'), index=False)

    print(table_summary_df.to_latex(index=False))
    print(table_summary_avarage_edit_classifications_low_high_requests_df.to_latex(index=False))
    print(table_summary_new_reusable_obsolete_df.to_latex(index=False))
    print(table_summary_low_high_mixed_tcs_df.to_latex(index=False))
