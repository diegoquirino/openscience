import data.utils as utils
import pandas as pd
import ast


class TestCasesAnalysis:
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
        # Labeling test cases affected by the added (new) steps or flows
        new_cts_df = self.merged_df[self.merged_df['new_ct'] > 0].groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            new_cts=('affected_cts', utils.union_sets)
        ).reset_index()
        # Uncomment for debug purposes: new_cts_df.to_csv('newcts.csv', index=False)
        # Labeling test cases affected by the updated/deleted (obsolete) steps or flows
        low_low_df = self.merged_df[(self.merged_df['obsolete_ct'] > 0) & (self.merged_df['edit_classification_truth'] == 'LOW') & (self.merged_df['edit_classification'] == 'LOW')]
        low_high_df = self.merged_df[(self.merged_df['obsolete_ct'] > 0) & (self.merged_df['edit_classification_truth'] == 'LOW') & (self.merged_df['edit_classification'] == 'HIGH')]
        high_df = self.merged_df[(self.merged_df['obsolete_ct'] > 0) & (self.merged_df['edit_classification_truth'] == 'HIGH')]
        low_low_grouped = low_low_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            obsolete_cts_low_low=('affected_cts', utils.union_sets)
        ).reset_index()
        low_high_grouped = low_high_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            obsolete_cts_low_high=('affected_cts', utils.union_sets)
        ).reset_index()
        high_grouped = high_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
            total_cts_size=('total_ct', 'max'),
            obsolete_cts_high=('affected_cts', utils.union_sets)
        ).reset_index()
        obsolete_cts_df = pd.merge(low_low_grouped, low_high_grouped,
                                   on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
        obsolete_cts_df = pd.merge(obsolete_cts_df, high_grouped,
                                   on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
        # Uncomment for debug purposes: obsolete_cts_df.to_csv('obsoletects.csv', index=False)
        # Create grouped_df from merging new and obsolete dataframes, processing and accounting test cases
        self.grouped_df = pd.merge(obsolete_cts_df, new_cts_df,
                                   on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
        self.grouped_df['new_cts_size'] = self.grouped_df['new_cts'].apply(utils.calculate_set_size)
        self.grouped_df['obsolete_cts_low_low_size'] = self.grouped_df['obsolete_cts_low_low'].apply(utils.calculate_set_size)
        self.grouped_df['obsolete_cts_low_high_size'] = self.grouped_df['obsolete_cts_low_high'].apply(utils.calculate_set_size)
        self.grouped_df['obsolete_cts_high_size'] = self.grouped_df['obsolete_cts_high'].apply(utils.calculate_set_size)
        self.grouped_df['obsolete_cts_size'] = self.grouped_df.apply(utils.union_set_size, axis=1)
        self.grouped_df['reusable_cts_size'] = self.grouped_df.apply(utils.get_min_positive_difference, axis=1)
        self.grouped_df = self.grouped_df[['filename', 'base_tag', 'head_tag',
                                           'new_cts', 'obsolete_cts_low_low', 'obsolete_cts_low_high', 'obsolete_cts_high',
                                           'obsolete_cts_low_low_size', 'obsolete_cts_low_high_size', 'obsolete_cts_high_size',
                                           'new_cts_size', 'obsolete_cts_size', 'reusable_cts_size', 'total_cts_size']]
        self.grouped_df['low_impacted_size'] = self.grouped_df.apply(utils.calculate_low_impacted_size, axis=1)
        self.grouped_df['high_impacted_size'] = self.grouped_df.apply(utils.calculate_high_impacted_size, axis=1)
        self.grouped_df['mixed_impacted_size'] = self.grouped_df.apply(utils.calculate_mixed_impacted_size, axis=1)
        # Uncomment for debug purposes: self.grouped_df.to_csv('grouped_result.csv', index=False)
        # Return merged_df and grouped_df
        return self.merged_df, self.grouped_df
