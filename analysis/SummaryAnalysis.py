import time

from analysis import ConfusionMatrixAnalysis, TestCasesAnalysis
import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt


class SummaryAnalysis:
    FINAL_SUMMARY_DF_COLUMNS = ['model', 'round', 'conf_matrix', 'tn', 'fp', 'fn', 'tp',
                                'total_classifications', 'total_lost_classifications',
                                'accuracy', 'recall', 'precision', 'f1', 'total_truth_class_low_priority_edits',
                                'total_truth_class_high_priority_edits', 'total_predicted_class_low_priority_edits',
                                'total_predicted_class_high_priority_edits',
                                'total_cts', 'total_new', 'total_obsolete', 'total_reusable',
                                'total_low_impacted', 'total_high_impacted',
                                'total_mixed_impacted',
                                'perc_saved_cts']

    def __init__(self):
        self.final_statistics_df = pd.DataFrame(columns=self.FINAL_SUMMARY_DF_COLUMNS)
        self.all_confusion_matrix_analysis = list()
        self.all_test_cases_analysis = list()

    def add(self, confusion_matrix_analysis: ConfusionMatrixAnalysis, test_cases_analysis: TestCasesAnalysis):
        self.all_confusion_matrix_analysis.append(confusion_matrix_analysis)
        self.all_test_cases_analysis.append(test_cases_analysis)

    def finalize(self):
        for index, cma in enumerate(self.all_confusion_matrix_analysis):
            tca = self.all_test_cases_analysis[index]
            total_cts = int(tca.grouped_df['new_cts_size'].sum() + tca.grouped_df['obsolete_cts_size'].sum() +
                            tca.grouped_df['reusable_cts_size'].sum())
            total_classifications = int(cma.tn) + int(cma.fp) + int(cma.fn) + int(cma.tp)
            new_row = {
                'model': cma.model,
                'round': cma.round,
                'conf_matrix': cma.conf_matrix,
                'tn': cma.tn,  # HIGH-HIGH
                'fp': cma.fp,  # HIGH-but predicted-LOW(p)
                'fn': cma.fn,  # LOW-but predicted-HIGH(n)
                'tp': cma.tp,  # LOW-LOW
                'total_classifications': total_classifications,
                'total_lost_classifications': int(0),
                'accuracy': cma.accuracy,
                'recall': cma.recall,
                'precision': cma.precision,
                'f1': cma.f1,
                'total_truth_class_low_priority_edits': cma.total_truth_classification_low_priority_edits,
                'total_truth_class_high_priority_edits': cma.total_truth_classification_high_priority_edits,
                'total_predicted_class_low_priority_edits': cma.total_result_classification_low_priority_edits,
                'total_predicted_class_high_priority_edits': cma.total_result_classification_high_priority_edits,
                'total_cts': total_cts,
                'total_new': int(tca.grouped_df['new_cts_size'].sum()),
                'total_obsolete': int(tca.grouped_df['obsolete_cts_size'].sum()),
                'total_reusable': int(tca.grouped_df['reusable_cts_size'].sum()),
                'total_low_impacted': int(tca.grouped_df['low_impacted_size'].sum()),
                'total_high_impacted': int(tca.grouped_df['high_impacted_size'].sum()),
                'total_mixed_impacted': int(tca.grouped_df['mixed_impacted_size'].sum()),
                # 'total_no_impacted': int(tca.grouped_df['reusable_cts_size'].sum()),
                'perc_saved_cts': float(tca.grouped_df['low_impacted_size'].sum()) / float(
                    tca.grouped_df['obsolete_cts_size'].sum()),
            }
            new_row_df = pd.DataFrame([new_row])
            self.final_statistics_df = pd.concat([self.final_statistics_df, new_row_df], ignore_index=True)
        max_total_classifications = self.final_statistics_df['total_classifications'].max()
        self.final_statistics_df['total_lost_classifications'] = (max_total_classifications -
                                                                  self.final_statistics_df['total_classifications'])
        # Convert COLS to float and round to 3 decimal places
        cols = ['accuracy', 'recall', 'precision', 'f1', 'perc_saved_cts']
        self.final_statistics_df[cols] = self.final_statistics_df[cols].astype(float).round(3)
        return self.final_statistics_df

    def plot_metrics(self):
        time.sleep(5)
        cols = ['model', 'accuracy', 'recall', 'precision', 'f1']
        num_cols = ['accuracy', 'recall', 'precision', 'f1']
        df_filtered = self.final_statistics_df[cols]
        df_filtered = df_filtered.copy()
        df_filtered[num_cols] = df_filtered[num_cols].apply(pd.to_numeric, errors='coerce')
        # Group by 'model' and calculate the average of numeric values
        df_grouped = df_filtered.groupby('model').mean().reset_index()
        # Round to 1 decimal place for the relevant columns
        df_grouped[num_cols] = df_grouped[num_cols].round(1)
        # Sort by 'recall', then by 'precision', then by 'f1'; lastly, 'accuracy'
        df_grouped = df_grouped.sort_values(by=['recall', 'precision', 'f1', 'accuracy'], ascending=True)
        # Set plot style to grayscale
        sns.set_theme(style='whitegrid')
        sns.set_palette('colorblind')
        # Melt the DataFrame to a long format for easy plotting with Seaborn
        df_melted = df_grouped.melt(id_vars='model', value_vars=['accuracy', 'recall', 'precision', 'f1'],
                                    var_name='Metric', value_name='Value')
        # Create the bar plot
        plt.figure(figsize=(10, 4))
        sns.barplot(x='model', y='Value', hue='Metric', data=df_melted)
        # Set plot labels and title
        plt.title('Performance Metrics by Model (Sorted by Recall, Precision, F1 and Accuracy)', fontsize=14)
        plt.xlabel('Text-generation LLM', fontsize=12)
        plt.ylabel('Average', fontsize=12)
        plt.xticks(rotation=45)
        plt.legend(title='Metric', loc='upper center', bbox_to_anchor=(0.5, 0.3), ncol=4)
        # Set y-axis scale with 0.1 increments
        plt.yticks(np.arange(0.0, 1.1, 0.1))
        # Show the plot
        plt.tight_layout()
        plt.show()

    def plot_lost_variance(self):
        time.sleep(5)
        df_filtered = self.final_statistics_df[['model', 'round', 'tn', 'fp', 'fn', 'tp',
                                                'total_classifications', 'total_lost_classifications']]
        df_filtered = df_filtered.copy()
        df_filtered['avg_low_impact_predicted'] = df_filtered['tp'] + df_filtered['fn']  # POSITIVE = LOW impact
        df_filtered['avg_high_impact_predicted'] = df_filtered['tn'] + df_filtered['fp']  # NEGATIVE = HIGH impact
        df_filtered = df_filtered.rename(columns={'total_classifications': 'avg_classifications'})
        df_filtered = df_filtered.rename(columns={'total_lost_classifications': 'avg_lost_classifications'})
        df_filtered = df_filtered.drop(columns=['round', 'tn', 'fp', 'fn', 'tp'])
        df_filtered = df_filtered.sort_values(by='avg_lost_classifications', ascending=True)
        # Generate a boxplot to show error variance among rounds for each model
        plt.figure(figsize=(6, 6))
        sns.boxplot(x='model', y='avg_lost_classifications', data=df_filtered)
        # Set plot style to grayscale
        sns.set_theme(style='whitegrid')
        sns.set_palette('colorblind')
        # Set plot labels and title
        plt.title('Lost Variance Among Rounds for Each Model', fontsize=14)
        plt.xlabel('Text-generation LLM', fontsize=12)
        plt.ylabel('Quantity', fontsize=12)
        plt.xticks(rotation=45)
        # Show the plot
        plt.tight_layout()
        plt.show()

    def view_table_avarage_edit_classifications_requests(self):
        df_filtered = self.final_statistics_df[['model', 'round', 'tn', 'fp', 'fn', 'tp',
                                                'total_classifications', 'total_lost_classifications']]
        df_filtered = df_filtered.copy()
        df_filtered['avg_low_impact_predicted'] = df_filtered['tp'] + df_filtered['fn']  # POSITIVE = LOW impact
        df_filtered['avg_high_impact_predicted'] = df_filtered['tn'] + df_filtered['fp']  # NEGATIVE = HIGH impact
        df_filtered = df_filtered.rename(columns={'total_classifications': 'avg_classifications'})
        df_filtered = df_filtered.rename(columns={'total_lost_classifications': 'avg_lost_classifications'})
        df_filtered = df_filtered.drop(columns=['round', 'tn', 'fp', 'fn', 'tp'])
        df_grouped = df_filtered.groupby('model').mean().reset_index()
        numeric_cols = ['avg_low_impact_predicted', 'avg_high_impact_predicted', 'avg_classifications',
                        'avg_lost_classifications']
        df_grouped[numeric_cols] = df_grouped[numeric_cols].apply(pd.to_numeric, errors='coerce')
        df_grouped = df_grouped.round(0)
        df_grouped[numeric_cols] = df_grouped[numeric_cols].astype(int)
        max_value = df_filtered['avg_classifications'].max()
        df_grouped['avg_perc'] = df_grouped['avg_lost_classifications'] / max_value * 100
        df_grouped['avg_perc'] = pd.to_numeric(df_grouped['avg_perc'], errors='coerce')
        df_grouped['avg_perc'] = df_grouped['avg_perc'].round(1)
        df_grouped = df_grouped.sort_values(by='avg_perc', ascending=True)
        return df_grouped[['model', 'avg_classifications', 'avg_lost_classifications', 'avg_perc']]

    def plot_low_lost_variance(self):
        df_filtered = self.final_statistics_df[['model', 'round', 'tn', 'fp', 'fn', 'tp',
                                                'total_truth_class_low_priority_edits',
                                                'total_truth_class_high_priority_edits']]
        df_filtered = df_filtered.copy()
        df_filtered['avg_low_impact_predicted'] = df_filtered['tp'] + df_filtered['fn']  # POSITIVE = LOW impact
        df_filtered['avg_high_impact_predicted'] = df_filtered['tn'] + df_filtered['fp']  # NEGATIVE = HIGH impact

        # Generate a boxplot to show error variance among rounds for each model
        plt.figure(figsize=(6, 6))
        sns.boxplot(x='model', y='avg_low_impact_predicted', data=df_filtered)
        # Set plot style to grayscale
        sns.set_theme(style='whitegrid')
        sns.set_palette('colorblind')
        # Set plot labels and title
        plt.title('Lost Variance Among Rounds for Each Model', fontsize=14)
        plt.xlabel('Text-generation LLM', fontsize=12)
        plt.ylabel('Quantity', fontsize=12)
        plt.xticks(rotation=45)
        # Show the plot
        plt.tight_layout()
        plt.show()

    def view_table_avarage_edit_classifications_low_high_requests(self):
        df_filtered = self.final_statistics_df[['model', 'round', 'tn', 'fp', 'fn', 'tp',
                                                'total_truth_class_low_priority_edits',
                                                'total_truth_class_high_priority_edits']]
        df_filtered = df_filtered.copy()
        df_filtered['avg_low_impact_predicted'] = df_filtered['tp'] + df_filtered['fn']  # POSITIVE = LOW impact
        df_filtered['avg_high_impact_predicted'] = df_filtered['tn'] + df_filtered['fp']  # NEGATIVE = HIGH impact
        df_filtered = df_filtered.drop(columns=['round', 'tn', 'fp', 'fn', 'tp'])
        df_filtered = df_filtered.rename(columns={'total_truth_class_low_priority_edits': 'avg_low_impact_truth'})
        df_filtered = df_filtered.rename(columns={'total_truth_class_high_priority_edits': 'avg_high_impact_truth'})
        numeric_cols = ['avg_low_impact_predicted', 'avg_high_impact_predicted', 'avg_low_impact_truth',
                        'avg_high_impact_truth']
        df_grouped = df_filtered.groupby('model').mean().reset_index()
        df_grouped[numeric_cols] = df_grouped[numeric_cols].apply(pd.to_numeric, errors='coerce')
        df_grouped = df_grouped.round(0)
        df_grouped[numeric_cols] = df_grouped[numeric_cols].astype(int)
        max_low_value = df_filtered['avg_low_impact_truth'].max()
        df_grouped['avg_low_perc'] = (1 - df_grouped['avg_low_impact_predicted'] / max_low_value) * 100
        df_grouped['avg_low_perc'] = pd.to_numeric(df_grouped['avg_low_perc'], errors='coerce')
        df_grouped['avg_low_perc'] = df_grouped['avg_low_perc'].round(1)
        max_high_value = df_filtered['avg_high_impact_truth'].max()
        df_grouped['avg_high_perc'] = (1 - df_grouped['avg_high_impact_predicted'] / max_high_value) * 100
        df_grouped['avg_high_perc'] = pd.to_numeric(df_grouped['avg_high_perc'], errors='coerce')
        df_grouped['avg_high_perc'] = df_grouped['avg_high_perc'].round(1)
        df_grouped = df_grouped.sort_values(by=['avg_low_perc', 'avg_high_perc'], ascending=True)
        return df_grouped[['model', 'avg_low_impact_predicted', 'avg_high_impact_predicted',
                           'avg_low_perc', 'avg_high_perc']]

    def plot_tp_by_model_across_rounds(self):
        columns_needed = ['model', 'tp']
        df_filtered = self.final_statistics_df[columns_needed]
        df_tp_max = df_filtered.groupby('model')['tp'].max().reset_index()
        sorted_models = df_tp_max.sort_values('tp')['model']
        df_tp_sorted = df_filtered[df_filtered['model'].isin(sorted_models)]
        df_tp_sorted['model'] = pd.Categorical(df_tp_sorted['model'], categories=sorted_models, ordered=True)
        # df_melted = df_filtered.melt(id_vars="model", var_name="Metric", value_name="Count")
        # df_melted.sort_values(by=['Count'], ascending=True)
        # Plot only the TP for the sorted models
        plt.figure(figsize=(6, 6))
        sns.boxplot(x="model", y="tp", data=df_tp_sorted)
        # plt.figure(figsize=(6, 6))
        # sns.boxplot(x="model", y="Count", hue="Metric", data=df_melted)
        # Enhance the plot with titles and labels
        plt.title('Variation of TP by Model across Rounds')
        plt.xlabel('Text-generation LLM', fontsize=12)
        plt.ylabel('Quantity')
        plt.xticks(rotation=45)
        # Display the plot
        plt.tight_layout()
        plt.show()

    def view_table_total_new_reusable_obsolete_test_cases_per_model_and_round(self):
        df_filtered = self.final_statistics_df[['model', 'round', 'total_new', 'total_obsolete',
                                                'total_reusable', 'total_cts']]
        df_filtered = df_filtered.copy()
        df_filtered = df_filtered[~df_filtered['model'].isin(['phi3', 'llama3.1'])]
        df_filtered['perc_new'] = (df_filtered['total_new'] / df_filtered['total_cts']).round(0).map("{:.0%}".format)
        df_filtered['perc_obsolete'] = (df_filtered['total_obsolete'] / df_filtered['total_cts']).round(0).map("{:.0%}".format)
        df_filtered['perc_reusable'] = (df_filtered['total_reusable'] / df_filtered['total_cts']).round(0).map("{:.0%}".format)
        return df_filtered

    def view_table_low_high_mixed_test_case_impacted(self):
        df_filtered = self.final_statistics_df[['model', 'round', 'total_low_impacted', 'total_high_impacted',
                                                'total_mixed_impacted', 'total_cts', 'perc_saved_cts']]
        df_filtered = df_filtered.copy()
        df_filtered = df_filtered[~df_filtered['model'].isin(['phi3', 'llama3.1'])]
        df_filtered['perc_saved_cts'] = df_filtered['perc_saved_cts'].map("{:.0%}".format)
        max_total_cts = df_filtered['total_cts'].max()
        df_filtered = df_filtered[df_filtered['total_cts'] == max_total_cts]
        df_filtered = df_filtered.drop('total_cts', axis=1)
        df_sorted = df_filtered.sort_values(by=['perc_saved_cts', 'total_low_impacted', 'round'], ascending=False)
        return df_sorted

