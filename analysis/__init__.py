import os
import ast
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from sklearn.metrics import confusion_matrix, accuracy_score, precision_score, recall_score, f1_score

LABELS = ['HIGH', 'LOW']
FINAL_SUMMARY_DF_COLUMNS = ['model', 'accuracy', 'recall', 'precision', 'f1',
                            'total_new_ct', 'total_obsolete_ct', 'total_reusable_ct', 'total_affected_ct',
                            'total_obsolete_both_low_ct']


class ConfusionMatrixAnalysis:
    def __init__(self, results_file, truth_file,
                 classification_result='classification_result', classification_truth='classification_truth',
                 conf_matrix_title='Confusion Matrix', accuracy_graph_title='Classification Accuracy'):
        self.f1 = None
        self.precision = None
        self.recall = None
        self.accuracy = None
        self.conf_matrix = None
        self.df_results = pd.read_csv(results_file)
        self.df_truth = pd.read_csv(truth_file)
        self.classification_result = classification_result
        self.classification_truth = classification_truth
        self.conf_matrix_title = conf_matrix_title
        self.accuracy_graph_title = accuracy_graph_title

    def calculate(self):
        # Ensure the data is aligned by index
        df_merged = pd.merge(self.df_results, self.df_truth, on=['index'], suffixes=('_result', '_truth'))
        df_merged[self.classification_truth] = (df_merged[self.classification_truth]
                                                .apply(lambda x: 1 if x == 'LOW' else 0))
        df_merged[self.classification_result] = (df_merged[self.classification_result]
                                                 .apply(lambda x: 1 if x == 'LOW' else 0))
        # Generate Confusion Matrix
        self.conf_matrix = confusion_matrix(df_merged[self.classification_truth],
                                            df_merged[self.classification_result])
        # Decompose Confusion Matrix
        tn, fp, fn, tp = self.conf_matrix.ravel()
        # Calculate Metrics
        self.accuracy = (tp + tn) / (tp + tn + fp + fn)
        self.recall = tp / (tp + fn)
        self.precision = tp / (tp + fp)
        self.f1 = 2 * (self.precision * self.recall) / (self.precision + self.recall)
        print(self.conf_matrix_title)
        print(self.conf_matrix)
        print(f'Accuracy: {self.accuracy} / Recall: {self.recall} / Precision: {self.precision} / F1: {self.f1}')
        return self.conf_matrix, self.accuracy, self.recall, self.precision, self.f1

    def plot(self):
        # Plot confusion matrix
        plt.figure(figsize=(10, 7))
        sns.heatmap(self.conf_matrix, annot=True, fmt='d', cmap='Blues',
                    annot_kws={"size": 24}, xticklabels=LABELS, yticklabels=LABELS)
        plt.tick_params(axis='both', which='major', labelsize=18)
        plt.xlabel('Predicted', fontsize=16)
        plt.ylabel('Truth', fontsize=16)
        plt.title(self.conf_matrix_title)
        plt.show()


def classify_edit_classification(group):
    unique_classes = group.unique()
    if len(unique_classes) == 1:
        return unique_classes[0]
    else:
        return 'LOW/HIGH'


def union_sets(sets):
    result = set()
    for s in sets:
        result = result.union(s)
    return result


class TestCasesEvaluation:
    def __init__(self, predicted_csv, truth_csv):
        self.predicted_df = pd.read_csv(predicted_csv)
        self.truth_df = pd.read_csv(truth_csv)
        self.merged_df = pd.DataFrame()
        self.grouped_df = pd.DataFrame()

    def merge_and_process(self):
        # Merge the dataframes on the keys 'index'
        self.merged_df = pd.merge(self.truth_df, self.predicted_df, on=['index'])
        # Convert 'affected_cts' from str to set
        self.merged_df['affected_cts'] = self.merged_df['affected_cts'].apply(ast.literal_eval)
        # Group the data by 'filename', 'base_tag', and 'head_tag', and aggregate the 'affected_cts' column into lists
        self.grouped_df = self.merged_df.groupby(['filename', 'base_tag', 'head_tag'])['affected_cts', 'edit_classification_truth'].agg(list).reset_index()
        # Create a new results dataframe
        df_result = pd.DataFrame(columns=['filename', 'base_tag', 'head_tag',
                                          'edit_classification_truth', 'edit_classification', 'total_ct',
                                          'affected_high_cts', 'affected_low_cts', 'affected_mixed_cts'])
        # Iterate over each row in the grouped dataframe and compute the 'affected_high_cts', 'affected_low_cts', and
        # 'affected_mixed_cts' columns based on the logic provided in the query
        for index, row in self.grouped_df.iterrows():
            affected_cts_high = set()
            affected_cts_low = set()
            for i in range(len(row['edit_classification_truth'])):
                if row['edit_classification_truth'][i] == 'HIGH':
                    affected_cts_high.update(row['affected_cts'][i])
                elif row['edit_classification_truth'][i] == 'LOW':
                    affected_cts_low.update(row['affected_cts'][i])
            affected_cts_mixed = affected_cts_high.intersection(affected_cts_low)
            affected_cts_high = affected_cts_high - affected_cts_mixed
            affected_cts_low = affected_cts_low - affected_cts_mixed
            df_result.loc[len(df_result)] = [row['filename'], row['base_tag'], row['head_tag'], 
                                             row['edit_classification_truth'], row['edit_classification'], 
                                             row['total_ct'], affected_cts_high, affected_cts_low, affected_cts_mixed]
        self.grouped_df = df_result
        # self.grouped_df = self.merged_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
        #     edit_classification_truth=('edit_classification_truth', classify_edit_classification),
        #     edit_classification=('edit_classification', classify_edit_classification),
        #     new_ct=('new_ct', 'max'),
        #     obsolete_ct=('obsolete_ct', 'max'),
        #     reusable_ct=('reusable_ct', 'min'),
        #     affected_cts=('affected_cts', union_sets),
        #     total_ct=('total_ct', 'max')
        # ).reset_index()
        # Calculate reusable cts based on total_ct and affected_cts
        # self.grouped_df['reusable_ct'] = self.grouped_df.apply(
        #     lambda row: max(0, row['total_ct'] - len(row['affected_cts'])),
        #     axis=1
        # )
        # # Create column obsolete_both_low_ct with total cts both use case edit classification "LOW" impact
        # self.grouped_df.loc[
        #     (self.grouped_df['edit_classification_truth'] == 'LOW') & (self.grouped_df['edit_classification'] == 'LOW'),
        #     'obsolete_both_low_ct'] = self.grouped_df['obsolete_ct']
        # self.grouped_df['obsolete_both_low_ct'] = self.grouped_df['obsolete_both_low_ct'].fillna(0)
        return self.merged_df, self.grouped_df

    def process_final_statistics(self, current_final_statistics_df, model, _accuracy, _recall, _precision, _f1):
        total = int(self.grouped_df['new_ct'].sum() + self.grouped_df['obsolete_ct'].sum() +
                    self.grouped_df['reusable_ct'].sum())
        new_row = {'model': model,
                   'accuracy': _accuracy,
                   'recall': _recall,
                   'precision': _precision,
                   'f1': _f1,
                   'total_new_ct': int(self.grouped_df['new_ct'].sum()),
                   'perc_new_ct': self.grouped_df['new_ct'].sum() / total * 100,
                   'total_obsolete_ct': int(self.grouped_df['obsolete_ct'].sum()),
                   'perc_obsolete_ct': self.grouped_df['obsolete_ct'].sum() / total * 100,
                   'total_reusable_ct': int(self.grouped_df['reusable_ct'].sum()),
                   'perc_reusable_ct': self.grouped_df['reusable_ct'].sum() / total * 100,
                   'total_affected_ct': total,
                   'total_obsolete_both_low_ct': int(self.grouped_df['obsolete_both_low_ct'].sum()),
                   'perc_obsolete_both_low_ct': self.grouped_df['obsolete_both_low_ct'].sum() / total * 100,
                   'perc_obsolete_both_low_obsolete_related_ct': self.grouped_df['obsolete_both_low_ct'].sum() /
                                                                 self.grouped_df['obsolete_ct'].sum() * 100
                   }
        new_row_df = pd.DataFrame([new_row])
        current_final_statistics_df = pd.concat([current_final_statistics_df, new_row_df], ignore_index=True)
        return current_final_statistics_df


CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH = '202408061530'
CSV_TRUTH_FILE_TO_BE_ANALYSED = 'rgp-diarias_diffs_cts_counted_Complete.csv'
DATAPATH = f'use_cases_edit_classifications{os.sep}{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}'
RESULTS_PATH = f'results{os.sep}{CURRENT_USE_CASES_EDIT_CLASSIFICATIONS_PATH}'

if __name__ == '__main__':
    truth_diffs_path = os.path.join(os.getcwd(), '..', 'data', CSV_TRUTH_FILE_TO_BE_ANALYSED)
    truth_df = pd.read_csv(truth_diffs_path)
    final_statistics_df = pd.DataFrame(columns=FINAL_SUMMARY_DF_COLUMNS)
    for use_cases_edit_classifications_file in os.listdir(os.path.join(os.getcwd(), '..', 'data', DATAPATH)):
        use_case_edit_classifications_file_path = os.path.join(os.path.join(os.getcwd(), '..', 'data', DATAPATH),
                                                               use_cases_edit_classifications_file)
        if os.path.isfile(use_case_edit_classifications_file_path):
            file_name_path, file_extension = os.path.splitext(use_case_edit_classifications_file_path)
            file_name = file_name_path.split(os.sep)[-1]
            confusion_matrix_view = ConfusionMatrixAnalysis(use_case_edit_classifications_file_path,
                                                            truth_diffs_path,
                                                            classification_result='edit_classification',
                                                            classification_truth='edit_classification_truth',
                                                            conf_matrix_title=f'{file_name.upper()} Confusion Matrix',
                                                            accuracy_graph_title=f'{file_name.upper()} Accuracy')
            conf_matrix, accuracy, recall, precision, f1 = confusion_matrix_view.calculate()
            confusion_matrix_view.plot()
            test_cases_eval = TestCasesEvaluation(use_case_edit_classifications_file_path, truth_diffs_path)
            mer_df, pro_df = test_cases_eval.merge_and_process()
            os.makedirs(f'{os.getcwd()}{os.sep}{RESULTS_PATH}', exist_ok=True)
            mer_df.to_csv(os.path.join(os.getcwd(), RESULTS_PATH, f'{file_name}-tc-merged-results.csv'), index=False)
            pro_df.to_csv(os.path.join(os.getcwd(), RESULTS_PATH, f'{file_name}-tc-processed-results.csv'), index=False)
            final_statistics_df = test_cases_eval.process_final_statistics(final_statistics_df, file_name, accuracy, recall, precision, f1)
    # Generate final statistics file
    final_statistics_df.to_csv(os.path.join(os.getcwd(), RESULTS_PATH, f'summary-metrics-results.csv'), index=False)
