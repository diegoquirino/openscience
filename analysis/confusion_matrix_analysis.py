import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from data.utils import log, uppercase_columns_values
from sklearn.metrics import confusion_matrix

# Define the labels for the confusion matrix
LABELS = ['HIGH', 'LOW']

class ConfusionMatrixAnalysis:
    """
    A class to analyze and visualize confusion matrices for classification results.
    """

    def __init__(self, results_file: str, truth_file: str, file_name: str,
                 classification_result: str = 'classification_result_column',
                 classification_truth: str = 'classification_truth_column'):
        """
        Initializes the ConfusionMatrixAnalysis with file paths and column names.

        :param results_file: Path to the CSV file containing classification results.
        :param truth_file: Path to the CSV file containing ground truth data.
        :param file_name: Name of the file for the model.
        :param classification_result: Column name for classification results.
        :param classification_truth: Column name for ground truth classifications.
        """
        self.results_file = results_file
        self.truth_file = truth_file
        self.file_name = file_name
        self.classification_result = classification_result
        self.classification_truth = classification_truth
        self.model = self._extract_model_name()
        self.round = self._extract_round_number()
        self.conf_matrix_title = self._define_conf_matrix_title()
        self.accuracy_graph_title = self._define_accuracy_graph_title()

        self.df_results = self._load_and_preprocess_results()
        self.df_truth = self._load_truth()
        self._calculate_totals()

        self.conf_matrix = None
        self.tn, self.fp, self.fn, self.tp = None, None, None, None

    def _load_and_preprocess_results(self) -> pd.DataFrame:
        """
        Loads and preprocesses the results DataFrame.

        :return: Preprocessed results DataFrame.
        """
        df_results = pd.read_csv(self.results_file)
        df_results = uppercase_columns_values(df_results, ['edit_classification'])
        return df_results[df_results['edit_classification'].isin(LABELS)]

    def _load_truth(self) -> pd.DataFrame:
        """
        Loads the ground truth DataFrame.

        :return: Ground truth DataFrame.
        """
        return pd.read_csv(self.truth_file)

    def _calculate_totals(self):
        """
        Calculates total counts for low and high priority edits in truth and results.
        """
        self.total_truth_classification_low_priority_edits = (self.df_truth[self.classification_truth] == 'LOW').sum()
        self.total_truth_classification_high_priority_edits = (self.df_truth[self.classification_truth] == 'HIGH').sum()
        self.total_result_classification_low_priority_edits = (self.df_results[self.classification_result] == 'LOW').sum()
        self.total_result_classification_high_priority_edits = (self.df_results[self.classification_result] == 'HIGH').sum()

    def calculate(self):
        """
        Calculates the confusion matrix and related metrics.
        """
        log(f'{self.conf_matrix_title}')

        df_merged = pd.merge(self.df_results, self.df_truth, on=['index'], suffixes=('_result', '_truth'))
        df_merged[self.classification_truth] = df_merged[self.classification_truth].apply(lambda x: 1 if x == 'LOW' else 0)
        df_merged[self.classification_result] = df_merged[self.classification_result].apply(lambda x: 1 if x == 'LOW' else 0)
        df_merged.dropna(subset=[self.classification_result], inplace=True)

        self.conf_matrix = confusion_matrix(df_merged[self.classification_truth], df_merged[self.classification_result])
        self.tn, self.fp, self.fn, self.tp = self.conf_matrix.ravel()
        self._print_metrics()
        return self.conf_matrix, self.accuracy, self.recall, self.precision, self.f1

    def _print_metrics(self):
        """
        Prints the calculated metrics to the console.
        """
        log(f'{self.conf_matrix} => TP={self.tp}, TN={self.tn}, FP={self.fp}, FN={self.fn}')
        log(f'Accuracy: {self.accuracy} / Recall: {self.recall} / Precision: {self.precision} / F1: {self.f1}')
        log(f'Total TRUTH [{self.classification_truth}] LOW priority edits: {self.total_truth_classification_low_priority_edits}')
        log(f'Total TRUTH [{self.classification_truth}] HIGH priority edits: {self.total_truth_classification_high_priority_edits}')
        log(f'Total RESULT [{self.classification_result}] LOW priority edits: {self.total_result_classification_low_priority_edits}')
        log(f'Total RESULT [{self.classification_result}] HIGH priority edits: {self.total_result_classification_high_priority_edits}')

    @property
    def accuracy(self) -> float:
        """
        Calculates the accuracy of the classification.

        :return: Accuracy value.
        """
        return (self.tp + self.tn) / (self.tp + self.tn + self.fp + self.fn)

    @property
    def recall(self) -> float:
        """
        Calculates the recall of the classification.

        :return: Recall value.
        """
        return self.tp / (self.tp + self.fn)

    @property
    def precision(self) -> float:
        """
        Calculates the precision of the classification.

        :return: Precision value.
        """
        return self.tp / (self.tp + self.fp)

    @property
    def f1(self) -> float:
        """
        Calculates the F1 score of the classification.

        :return: F1 score value.
        """
        return 2 * (self.precision * self.recall) / (self.precision + self.recall)

    def plot(self):
        """
        Plots the confusion matrix using seaborn.
        """
        plt.figure(figsize=(10, 7))
        sns.heatmap(self.conf_matrix, annot=True, fmt='d', cmap='Blues',
                    annot_kws={'size': 24}, xticklabels=LABELS, yticklabels=LABELS)
        plt.tick_params(axis='both', which='major', labelsize=18)
        plt.xlabel('Predicted', fontsize=16)
        plt.ylabel('Truth', fontsize=16)
        plt.title(self.conf_matrix_title)
        plt.show()

    def _extract_model_name(self) -> str:
        # Find the index of 'results' which marks the end of the model name
        parts = self.file_name.split('-')
        try:
            results_index = parts.index('results')
            # Join all parts before 'results'
            return '-'.join(parts[:results_index])
        except ValueError:
            # If 'results' is not found, return the first two parts as before
            return '-'.join(parts[:2])

    def _define_conf_matrix_title(self) -> str:
        return f'{self.model.upper()}-{self.round} Confusion Matrix'

    def _define_accuracy_graph_title(self) -> str:
        return f'{self.model.upper()}-{self.round} Accuracy'

    def _extract_round_number(self):
        # Split the string by '-' and get the last element
        last_part = self.file_name.split('-')[-1]
        # Convert to integer
        return int(last_part)