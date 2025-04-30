from data.utils import union_sets, calculate_set_size, union_set_size, get_min_positive_difference, \
    calculate_low_impacted_size, calculate_high_impacted_size, calculate_mixed_impacted_size
import pandas as pd
import ast

class TestCasesAnalysis:
    def __init__(self, predicted_csv, truth_csv):
        """
        Initialize the TestCasesAnalysis class with predicted and truth CSV files.

        :param predicted_csv: Path to the CSV file containing predicted data.
        :param truth_csv: Path to the CSV file containing truth data.
        """
        self.predicted_df = pd.read_csv(predicted_csv)
        self.truth_df = pd.read_csv(truth_csv)
        self.merged_df = pd.DataFrame()
        self.grouped_df = pd.DataFrame()

    def merge_and_process(self):
        """
        Merge the predicted and truth dataframes, process the data to label new and obsolete test cases,
        and calculate various sizes.

        :return: Tuple containing the merged dataframe and the grouped dataframe.
        """
        self.merged_df = self._merge_dataframes()
        self.merged_df['affected_cts'] = self.merged_df['affected_cts'].apply(ast.literal_eval)

        new_cts_df = self._label_new_cts()
        obsolete_cts_df = self._label_obsolete_cts()

        self.grouped_df = self._merge_new_and_obsolete(new_cts_df, obsolete_cts_df)
        self._calculate_sizes()

        return self.merged_df, self.grouped_df

    def _merge_dataframes(self):
        """
        Merge the truth and predicted dataframes on the 'index' column.

        :return: Merged dataframe.
        """
        return pd.merge(self.truth_df, self.predicted_df, on=['index'])

    def _label_new_cts(self):
        """
        Label new test cases by grouping the merged dataframe and aggregating the affected test cases.

        :return: Dataframe containing labeled new test cases.
        """
        return self.merged_df[self.merged_df['new_ct'] > 0].groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            new_cts=('affected_cts', union_sets)
        ).reset_index()

    def _label_obsolete_cts(self):
        """
        Label obsolete test cases by filtering and grouping the merged dataframe based on classification.

        :return: Dataframe containing labeled obsolete test cases.
        """
        low_low_df = self._filter_obsolete_cts('LOW', 'LOW')
        low_high_df = self._filter_obsolete_cts('LOW', 'HIGH')
        high_df = self._filter_obsolete_cts('HIGH', None)

        low_low_grouped = self._group_obsolete_cts(low_low_df, 'obsolete_cts_low_low')
        low_high_grouped = self._group_obsolete_cts(low_high_df, 'obsolete_cts_low_high')
        high_grouped = self._group_obsolete_cts(high_df, 'obsolete_cts_high')

        obsolete_cts_df = pd.merge(low_low_grouped, low_high_grouped,
                                   on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
        return pd.merge(obsolete_cts_df, high_grouped, on=['filename', 'base_tag', 'head_tag', 'total_cts_size'],
                        how='outer')

    def _filter_obsolete_cts(self, truth_class, pred_class):
        """
        Filter obsolete test cases based on truth and predicted classifications.

        :param truth_class: The truth classification to filter by.
        :param pred_class: The predicted classification to filter by.
        :return: Filtered dataframe.
        """
        if pred_class:
            return self.merged_df[(self.merged_df['obsolete_ct'] > 0) &
                                  (self.merged_df['edit_classification_truth'] == truth_class) &
                                  (self.merged_df['edit_classification'] == pred_class)]
        return self.merged_df[(self.merged_df['obsolete_ct'] > 0) &
                              (self.merged_df['edit_classification_truth'] == truth_class)]

    def _group_obsolete_cts(self, df, col_name):
        """
        Group obsolete test cases by filename, base tag, and head tag, and aggregate the affected test cases.

        :param df: Dataframe to group.
        :param col_name: Column name for the aggregated affected test cases.
        :return: Grouped dataframe.
        """
        return df.groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            **{col_name: ('affected_cts', union_sets)}
        ).reset_index()

    def _merge_new_and_obsolete(self, new_cts_df, obsolete_cts_df):
        """
        Merge the new and obsolete test cases dataframes.

        :param new_cts_df: Dataframe containing new test cases.
        :param obsolete_cts_df: Dataframe containing obsolete test cases.
        :return: Merged dataframe.
        """
        grouped_df = pd.merge(obsolete_cts_df, new_cts_df, on=['filename', 'base_tag', 'head_tag', 'total_cts_size'],
                              how='outer')
        return grouped_df

    def _calculate_sizes(self):
        """
        Calculate various sizes for the grouped dataframe, including new, obsolete, and reusable test cases sizes.
        """
        self.grouped_df['new_cts_size'] = self.grouped_df['new_cts'].apply(calculate_set_size)
        self.grouped_df['obsolete_cts_low_low_size'] = self.grouped_df['obsolete_cts_low_low'].apply(
            calculate_set_size)
        self.grouped_df['obsolete_cts_low_high_size'] = self.grouped_df['obsolete_cts_low_high'].apply(
            calculate_set_size)
        self.grouped_df['obsolete_cts_high_size'] = self.grouped_df['obsolete_cts_high'].apply(calculate_set_size)
        self.grouped_df['obsolete_cts_size'] = self.grouped_df.apply(union_set_size, axis=1)
        self.grouped_df['reusable_cts_size'] = self.grouped_df.apply(get_min_positive_difference, axis=1)
        self.grouped_df = self.grouped_df[['filename', 'base_tag', 'head_tag',
                                           'new_cts', 'obsolete_cts_low_low', 'obsolete_cts_low_high',
                                           'obsolete_cts_high',
                                           'obsolete_cts_low_low_size', 'obsolete_cts_low_high_size',
                                           'obsolete_cts_high_size',
                                           'new_cts_size', 'obsolete_cts_size', 'reusable_cts_size', 'total_cts_size']]
        self.grouped_df['low_impacted_size'] = self.grouped_df.apply(calculate_low_impacted_size, axis=1)
        self.grouped_df['high_impacted_size'] = self.grouped_df.apply(calculate_high_impacted_size, axis=1)
        self.grouped_df['mixed_impacted_size'] = self.grouped_df.apply(calculate_mixed_impacted_size, axis=1)