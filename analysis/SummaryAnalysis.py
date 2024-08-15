from analysis import ConfusionMatrixAnalysis, TestCasesAnalysis
import pandas as pd


class SummaryAnalysis:
    FINAL_SUMMARY_DF_COLUMNS = ['model', 'round', 'conf_matrix', 'tn', 'fp', 'fn', 'tp',
                                'total_classifications', 'total_lost_classifications',
                                'accuracy', 'recall', 'precision', 'f1', 'total_cts',
                                'total_new', 'total_obsolete', 'total_reusable',
                                'total_low_impacted', 'total_high_impacted',
                                'total_mixed_impacted', 'total_no_impacted',
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
                'total_cts': total_cts,
                'total_new': int(tca.grouped_df['new_cts_size'].sum()),
                'total_obsolete': int(tca.grouped_df['obsolete_cts_size'].sum()),
                'total_reusable': int(tca.grouped_df['reusable_cts_size'].sum()),
                'total_low_impacted': int(tca.grouped_df['low_impacted_size'].sum()),
                'total_high_impacted': int(tca.grouped_df['high_impacted_size'].sum()),
                'total_mixed_impacted': int(tca.grouped_df['mixed_impacted_size'].sum()),
                'total_no_impacted': int(tca.grouped_df['reusable_cts_size'].sum()),
                'perc_saved_cts': float(tca.grouped_df['low_impacted_size'].sum()) / float(tca.grouped_df['obsolete_cts_size'].sum()),
            }
            new_row_df = pd.DataFrame([new_row])
            self.final_statistics_df = pd.concat([self.final_statistics_df, new_row_df], ignore_index=True)
        max_total_classifications = self.final_statistics_df['total_classifications'].max()
        self.final_statistics_df['total_lost_classifications'] = (max_total_classifications -
                                                                  self.final_statistics_df['total_classifications'])
        return self.final_statistics_df

    def plot(self):
        print('Not yet implemented')
        print(self.final_statistics_df)
