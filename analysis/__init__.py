import os
from data.utils import get_key
from .confusion_matrix_analysis import ConfusionMatrixAnalysis
from .test_cases_analysis import TestCasesAnalysis
from .summary_analysis import SummaryAnalysis

os.environ['ANALYSIS_PATH'] = get_key('ANALYSIS_PATH')