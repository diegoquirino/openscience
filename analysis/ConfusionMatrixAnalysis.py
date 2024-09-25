import time

import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import data.utils as utils
from sklearn.metrics import confusion_matrix


class ConfusionMatrixAnalysis:
    LABELS = ['HIGH', 'LOW']

    def __init__(self, results_file, truth_file,
                 classification_result='classification_result', classification_truth='classification_truth',
                 conf_matrix_title='Confusion Matrix', accuracy_graph_title='Classification Accuracy'):
        self.f1 = None
        self.precision = None
        self.recall = None
        self.accuracy = None
        self.conf_matrix = None
        self.tn, self.fp, self.fn, self.tp = None, None, None, None
        self.df_results = pd.read_csv(results_file)
        self.model, self.round = utils.extract_model_and_round(results_file)
        self.df_truth = pd.read_csv(truth_file)
        self.classification_result = classification_result
        self.classification_truth = classification_truth
        self.conf_matrix_title = conf_matrix_title
        self.accuracy_graph_title = accuracy_graph_title
        self.total_truth_classification_low_priority_edits = (self.df_truth[self.classification_truth] == 'LOW').sum()
        self.total_truth_classification_high_priority_edits = (self.df_truth[self.classification_truth] == 'HIGH').sum()
        self.total_result_classification_low_priority_edits = (self.df_results[self.classification_result] == 'LOW').sum()
        self.total_result_classification_high_priority_edits = (self.df_results[self.classification_result] == 'HIGH').sum()

    def calculate(self):
        print(f'{self.conf_matrix_title}')
        # Ensure the data is aligned by index
        df_merged = pd.merge(self.df_results, self.df_truth, on=['index'], suffixes=('_result', '_truth'))
        df_merged[self.classification_truth] = (df_merged[self.classification_truth]
                                                .apply(lambda x: 1 if x == 'LOW' else 0))
        df_merged[self.classification_result] = (df_merged[self.classification_result]
                                                 .apply(lambda x: 1 if x == 'LOW' else 0))
        # Generate Confusion Matrix
        df_merged.dropna(subset=[self.classification_result], inplace=True)
        # df_merged_filtered.to_csv('teste.csv')
        self.conf_matrix = confusion_matrix(df_merged[self.classification_truth],
                                            df_merged[self.classification_result])
        # Decompose Confusion Matrix
        self.tn, self.fp, self.fn, self.tp = self.conf_matrix.ravel()
        # Calculate Metrics
        self.accuracy = (self.tp + self.tn) / (self.tp + self.tn + self.fp + self.fn)
        self.recall = self.tp / (self.tp + self.fn)
        self.precision = self.tp / (self.tp + self.fp)
        self.f1 = 2 * (self.precision * self.recall) / (self.precision + self.recall)
        # Print log to console
        print(f'{self.conf_matrix} => TP={self.tp}, TN={self.tn}, FP={self.fp}, FN={self.fn}')
        print(f'Accuracy: {self.accuracy} / Recall: {self.recall} / Precision: {self.precision} / F1: {self.f1}')
        print(f'Total TRUTH [{self.classification_truth}] LOW priority edits: {self.total_truth_classification_low_priority_edits}')
        print(f'Total TRUTH [{self.classification_truth}] HIGH priority edits: {self.total_truth_classification_high_priority_edits}')
        print(f'Total RESULT [{self.classification_result}] LOW priority edits: {self.total_result_classification_low_priority_edits}')
        print(f'Total RESULT [{self.classification_result}] HIGH priority edits: {self.total_result_classification_high_priority_edits}')
        return self.conf_matrix, self.accuracy, self.recall, self.precision, self.f1

    def plot(self):
        # Plot confusion matrix
        plt.figure(figsize=(10, 7))
        sns.heatmap(self.conf_matrix, annot=True, fmt='d', cmap='Blues',
                    annot_kws={"size": 24}, xticklabels=self.LABELS, yticklabels=self.LABELS)
        plt.tick_params(axis='both', which='major', labelsize=18)
        plt.xlabel('Predicted', fontsize=16)
        plt.ylabel('Truth', fontsize=16)
        plt.title(self.conf_matrix_title)
        plt.show()
