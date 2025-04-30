from abc import ABC, abstractmethod
import pandas as pd

# Define the column names for the output DataFrame
OUTPUT_DF_COLUMN_NAMES = ['edit_classification', 'decision_rationale', 'elapsed_time_ms']

# Abstract Class for Strategy
class EditClassifierStrategy(ABC):
    def __init__(self):
        self.df = self.reset_df()
    @abstractmethod
    def classify(self, origin: str, target: str) -> pd.DataFrame:
        pass
    def reset_df(self):
        self.df = pd.DataFrame(columns=OUTPUT_DF_COLUMN_NAMES)
        return self.df
    def get_df(self):
        return self.df