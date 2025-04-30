"""
File: `main_analysis_plot_and_summary.py`
Purpose: Load and analyze classification results, compute metrics, and generate summary outputs.
"""

from analysis import ConfusionMatrixAnalysis, TestCasesAnalysis, SummaryAnalysis
from data.utils import get_software_data, log, csv_and_xlsx_and_view_latex
import pandas as pd
import os

if __name__ == '__main__':
    # Retrieve project-specific data from configuration
    software, prefix, versions, turns, tcs_strategy, uc_prefix = get_software_data()

    # Gather paths for reading classification results and writing analysis outputs
    current_use_cases_edit_classification_path = os.environ['ANALYSIS_PATH']
    datapath = os.path.join(
        os.getcwd(),
        'data',
        f'sw_{software}',
        'use_cases_edit_classifications',
        current_use_cases_edit_classification_path
    )
    analysis_results_path = os.path.join(
        os.getcwd(),
        'analysis',
        'results',
        current_use_cases_edit_classification_path
    )
    truth_diffs_path = os.path.join(
        os.getcwd(),
        'data',
        f'sw_{software}',
        f'{software}_diffs_counted_{tcs_strategy}.csv'
    )

    # Load ground truth data
    truth_df = pd.read_csv(truth_diffs_path)

    # Create a summary object to accumulate metrics
    summary_analysis = SummaryAnalysis()

    # Identify all classification result files to be analyzed
    for use_cases_edit_classifications_file in os.listdir(datapath):
        use_case_edit_classifications_file_path = os.path.join(datapath, use_cases_edit_classifications_file)

        # Process only actual files (not subdirectories)
        if os.path.isfile(use_case_edit_classifications_file_path):
            file_name_path, file_extension = os.path.splitext(use_case_edit_classifications_file_path)
            file_name = file_name_path.split(os.sep)[-1]

            # Initialize confusion matrix analysis for this classification file
            confusion_matrix_analysis = ConfusionMatrixAnalysis(
                use_case_edit_classifications_file_path,
                truth_diffs_path,
                file_name=file_name,
                classification_result='edit_classification',
                classification_truth='edit_classification_truth'
            )

            # Calculate and plot confusion matrix, then gather metrics
            conf_matrix, accuracy, recall, precision, f1 = confusion_matrix_analysis.calculate()
            confusion_matrix_analysis.plot()

            # Perform further test case analysis
            test_cases_analysis = TestCasesAnalysis(use_case_edit_classifications_file_path, truth_diffs_path)
            merged_df, grouped_df = test_cases_analysis.merge_and_process()

            # Ensure the results directory exists and save the merged analysis results
            os.makedirs(analysis_results_path, exist_ok=True)
            merged_df.to_csv(os.path.join(analysis_results_path, f'{file_name}-tc-merged-results.csv'), index=False)
            grouped_df.to_csv(os.path.join(analysis_results_path, f'{file_name}-tc-grouped-results.csv'), index=False)

            # Collect results into the summary object
            summary_analysis.add(confusion_matrix_analysis, test_cases_analysis)

    # Finalize the summary metrics for all processed files
    summary_df = summary_analysis.finalize()
    csv_and_xlsx_and_view_latex(
        summary_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-summary-results'
        )
    )

    # Create a condensed summary table with key metrics
    summary_df_resumed = summary_df[['model', 'round', 'precision', 'recall', 'f1', 'accuracy']]
    summary_df_resumed_avg = summary_df_resumed.groupby('model', as_index=False)[
        ['precision', 'recall', 'f1', 'accuracy']
    ].mean()
    summary_df_resumed_avg = summary_df_resumed_avg.sort_values(by=['precision', 'recall'], ascending=[False, False])
    csv_and_xlsx_and_view_latex(
        summary_df_resumed_avg,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-summary-resumed-avg-precision-recall-results'
        ),
        'precision'
    )

    # Sort by F1 score for further comparison
    summary_df_resumed_avg = summary_df_resumed_avg.sort_values(by=['f1'], ascending=[False])
    csv_and_xlsx_and_view_latex(
        summary_df_resumed_avg,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-summary-resumed-avg-f1-results'
        ),
        'f1'
    )

    # Generate various plots for the analysis summary
    summary_analysis.plot_metrics()
    summary_analysis.plot_lost_variance()
    summary_analysis.plot_tp_by_model_across_rounds()

    # Produce additional tables on test case classifications
    table_summary_df = summary_analysis.view_table_avarage_edit_classifications_requests()
    csv_and_xlsx_and_view_latex(
        table_summary_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-total-classifications-results'
        )
    )

    # Summaries for low-high edit classification requests
    table_summary_avarage_edit_classifications_low_high_requests_df = summary_analysis.view_table_avarage_edit_classifications_low_high_requests()
    csv_and_xlsx_and_view_latex(
        table_summary_avarage_edit_classifications_low_high_requests_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-avarage-edit-classifications-low-high-requests-results'
        )
    )

    # Summaries for new, reusable, and obsolete test cases
    table_summary_new_reusable_obsolete_df = summary_analysis.view_table_total_new_reusable_obsolete_test_cases_per_model_and_round()
    csv_and_xlsx_and_view_latex(
        table_summary_new_reusable_obsolete_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-total-new-reusable-obsolete-tcs-results'
        )
    )

    # Summaries for low, high, and mixed impacted test cases
    table_summary_low_high_mixed_tcs_df = summary_analysis.view_table_low_high_mixed_test_case_impacted()
    csv_and_xlsx_and_view_latex(
        table_summary_low_high_mixed_tcs_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-total-low-high-mixed-tcs-results'
        ),
        'total_low_impacted'
    )

    # Filter selected models and reorder results by percentage of saved test cases
    selected_models = ["Claude-3.5-sonnet", "DeepSeek-V3", "GPT-o1-mini", "Mixtral", "Llama-3.3", "GPT-4o", "Gemma-2"]
    table_summary_low_high_mixed_tcs_df_filtered = table_summary_low_high_mixed_tcs_df[
        table_summary_low_high_mixed_tcs_df["model"].str.startswith(tuple(selected_models))
    ]
    columns_lhm_tcs = [
        'model',
        'total_low_impacted',
        'total_high_impacted',
        'total_mixed_impacted',
        'perc_saved_cts'
    ]
    table_summary_low_high_mixed_tcs_df_dropped = table_summary_low_high_mixed_tcs_df_filtered.drop(columns=['round'])
    table_summary_low_high_mixed_tcs_df_grouped = table_summary_low_high_mixed_tcs_df_dropped.groupby(
        columns_lhm_tcs,
        as_index=False
    ).size()
    table_summary_low_high_mixed_tcs_df_grouped['perc_saved_cts'] = table_summary_low_high_mixed_tcs_df_grouped[
        'perc_saved_cts'
    ].str.replace('%', '').astype(int)
    table_summary_low_high_mixed_tcs_df_sorted = table_summary_low_high_mixed_tcs_df_grouped.sort_values(
        by='perc_saved_cts',
        ascending=False
    )
    table_summary_low_high_mixed_tcs_df_sorted['perc_saved_cts'] = (
                                                                       table_summary_low_high_mixed_tcs_df_sorted[
                                                                           'perc_saved_cts']
                                                                   ).astype(str) + '%'

    # Save filtered and sorted data for selected models
    csv_and_xlsx_and_view_latex(
        table_summary_low_high_mixed_tcs_df_sorted,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-total-low-high-mixed-tcs-selected-models-results'
        ),
        'total_low_impacted'
    )

    # Table summaries for average accuracy, recall, precision, and F1 metrics
    table_summary_average_accuracy_df = summary_analysis.view_table_average_accuracy()
    csv_and_xlsx_and_view_latex(
        table_summary_average_accuracy_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-average-accuracy-results'
        ),
        'average_accuracy'
    )

    table_summary_average_recall_df = summary_analysis.view_table_average_recall()
    csv_and_xlsx_and_view_latex(
        table_summary_average_recall_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-average-recall-results'
        ),
        'average_recall'
    )

    table_summary_average_precision_df = summary_analysis.view_table_average_precision()
    csv_and_xlsx_and_view_latex(
        table_summary_average_precision_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-average-precision-results'
        ),
        'average_precision'
    )

    table_summary_average_f1_df = summary_analysis.view_table_average_f1()
    csv_and_xlsx_and_view_latex(
        table_summary_average_f1_df,
        os.path.join(
            analysis_results_path,
            f'{current_use_cases_edit_classification_path}-{software}-table-average-f1-results'
        ),
        'average_f1'
    )