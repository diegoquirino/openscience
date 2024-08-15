from abc import ABC, abstractmethod
import pandas as pd


class LLMStrategy(ABC):
    """
    An abstract base class defining the interface for Language Model (LLM) strategies.

    This class serves as a blueprint for concrete strategy implementations, ensuring they provide
    a `classify_change` method that adheres to a specific contract.
    """
    OUTPUT_DF_COLUMN_NAMES = ['edit_classification', 'decision_rationale', 'elapsed_time_ms']

    @abstractmethod
    def classify_change(self, origin: str, target: str) -> pd.DataFrame:
        """
        Classifies the change between two strings.

        Args:
            origin: The original string.
            target: The modified string.

        Returns:
            A string representing the classification of the change.
        """
        pass
