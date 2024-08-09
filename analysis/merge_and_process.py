import pandas as pd
import ast
import os


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


def union_set_size(row):
    union_set = set()
    for col in ['obsolete_cts_low_low', 'obsolete_cts_low_high', 'obsolete_cts_high']:
        if pd.notna(row[col]):
            union_set.update(row[col])
    return len(union_set)


def calculate_set_size(value):
    if pd.isna(value):
        return 0
    elif isinstance(value, set):
        return len(value)
    elif isinstance(value, str):
        try:
            value_set = eval(value)
            return len(value_set)
        except:
            return 0
    else:
        return 0


def get_min_positive_difference(row):
    differences = []
    for col in ['new_cts_size', 'obsolete_cts_size']:
        diff = row['total_cts_size'] - row[col]
        if diff > 0:
            differences.append(diff)
        elif diff == 0:
            differences.append(0)
    return min(differences) if differences else float('nan')


def calculate_low_impacted_size(row):
    # test cases that include updated steps classified by our strategy as 'low impact'
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    return len((obsolete_cts_low_low - obsolete_cts_low_high)
               & (obsolete_cts_low_low - obsolete_cts_high)
               & (obsolete_cts_low_low - new_cts))


def calculate_high_impacted_size(row):
    # test cases that include 'high impact' steps
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    high_united = obsolete_cts_high.union(new_cts)
    return len((high_united - obsolete_cts_low_low)
               & (high_united - obsolete_cts_low_high))


def calculate_mixed_impacted_size(row):
    # test cases that include at least one 'high impact' step and at least one 'low impact' step
    new_cts = row['new_cts'] if pd.notna(row['new_cts']) else set()
    obsolete_cts_low_low = row['obsolete_cts_low_low'] if pd.notna(row['obsolete_cts_low_low']) else set()
    obsolete_cts_low_high = row['obsolete_cts_low_high'] if pd.notna(row['obsolete_cts_low_high']) else set()
    obsolete_cts_high = row['obsolete_cts_high'] if pd.notna(row['obsolete_cts_high']) else set()
    low_united = obsolete_cts_low_low.union(obsolete_cts_low_high)
    high_united = obsolete_cts_high.union(new_cts)
    return len(low_united.intersection(high_united))



################################################################
# merged_csv = os.path.join(os.getcwd(), 'test_merged.csv')
merged_csv = os.path.join(os.getcwd(), 'results', '202408061530', 'mixtral-results-0-tc-merged-results.csv')
merged_df = pd.read_csv(merged_csv)
merged_df['affected_cts'] = merged_df['affected_cts'].apply(ast.literal_eval)
# Labeling test cases affected by the added (new) steps or flows
new_cts_df = merged_df[merged_df['new_ct'] > 0].groupby(['filename', 'base_tag', 'head_tag']).agg(
    total_cts_size=('total_ct', 'max'),
    new_cts=('affected_cts', union_sets)
).reset_index()
new_cts_df.to_csv('newcts.csv', index=False)
# ############################################################################## #
low_low_df = merged_df[(merged_df['obsolete_ct'] > 0) & (merged_df['edit_classification_truth'] == 'LOW') & (
            merged_df['edit_classification'] == 'LOW')]
low_high_df = merged_df[(merged_df['obsolete_ct'] > 0) & (merged_df['edit_classification_truth'] == 'LOW') & (
            merged_df['edit_classification'] == 'HIGH')]
high_df = merged_df[(merged_df['obsolete_ct'] > 0) & (merged_df['edit_classification_truth'] == 'HIGH')]
# ############################################################################## #
low_low_grouped = low_low_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
    total_cts_size=('total_ct', 'max'),
    obsolete_cts_low_low=('affected_cts', union_sets)
).reset_index()
low_high_grouped = low_high_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
    total_cts_size=('total_ct', 'max'),
    obsolete_cts_low_high=('affected_cts', union_sets)
).reset_index()
high_grouped = high_df.groupby(['filename', 'base_tag', 'head_tag']).agg(
    total_cts_size=('total_ct', 'max'),
    obsolete_cts_high=('affected_cts', union_sets)
).reset_index()
# ############################################################################## #
obsolete_cts_df = pd.merge(low_low_grouped, low_high_grouped,
                           on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
obsolete_cts_df = pd.merge(obsolete_cts_df, high_grouped,
                           on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
obsolete_cts_df.to_csv('obsoletects.csv', index=False)
# ############################################################################## #
results_df = pd.merge(obsolete_cts_df, new_cts_df,
                      on=['filename', 'base_tag', 'head_tag', 'total_cts_size'], how='outer')
results_df.to_csv('result.csv', index=False)
# ############################################################################## #
results_df['new_cts_size'] = results_df['new_cts'].apply(calculate_set_size)
results_df['obsolete_cts_low_low_size'] = results_df['obsolete_cts_low_low'].apply(calculate_set_size)
results_df['obsolete_cts_low_high_size'] = results_df['obsolete_cts_low_high'].apply(calculate_set_size)
results_df['obsolete_cts_high_size'] = results_df['obsolete_cts_high'].apply(calculate_set_size)
results_df['obsolete_cts_size'] = results_df.apply(union_set_size, axis=1)
results_df['reusable_cts_size'] = results_df.apply(get_min_positive_difference, axis=1)
# filename,base_tag,head_tag,total_cts_size,obsolete_cts_low_low,obsolete_cts_low_high,obsolete_cts_high,new_cts,new_cts_size,obsolete_cts_low_low_size,obsolete_cts_low_high_size,obsolete_cts_high_size,reusable_cts_size,obsolete_cts_size
results_df = results_df[['filename', 'base_tag', 'head_tag',
                         'new_cts', 'obsolete_cts_low_low', 'obsolete_cts_low_high', 'obsolete_cts_high',
                         'obsolete_cts_low_low_size', 'obsolete_cts_low_high_size', 'obsolete_cts_high_size',
                         'new_cts_size', 'obsolete_cts_size', 'reusable_cts_size', 'total_cts_size']]
results_df['low_impacted_size'] = results_df.apply(calculate_low_impacted_size, axis=1)
results_df['high_impacted_size'] = results_df.apply(calculate_high_impacted_size, axis=1)
results_df['mixed_impacted_size'] = results_df.apply(calculate_mixed_impacted_size, axis=1)
results_df.to_csv('result.csv', index=False)

# grouped_df = merged_df.groupby(['filename', 'base_tag', 'head_tag', 'edit_classification_truth', 'edit_classification']).agg(
#     new_ct=('new_ct', 'max'),
#     obsolete_ct=('obsolete_ct', 'max'),
#     reusable_ct=('reusable_ct', 'min'),
#     total_ct=('total_ct', 'max'),
#     affected_cts=('affected_cts', union_sets),
# ).reset_index()
#
# grouped_df.to_csv('grouped.csv', index=False)
# index, filename, base_tag,head_tag, base_tag_lines_txt ,head_tag_lines_txt, edit_classification_truth,
# new_ct, obsolete_ct, reusable_ct, affected_cts, total_ct, edit_classification, decision_rationale, elapsed_time_ms
