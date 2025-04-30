import pandas as pd
from data.utils import log, clear
from strategy import EditClassifierStrategy
from strsimpy.normalized_levenshtein import NormalizedLevenshtein
from strsimpy.ngram import NGram
from strsimpy.jaccard import Jaccard
from strsimpy.cosine import Cosine
from strsimpy.sorensen_dice import SorensenDice
from time import time

# Concrete Strategies for Distance Functions
class DistanceFunctionClassifierStrategy(EditClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__()
        self.impact_threshold = impact_threshold

    def classify_with_distance_function(self, origin: str, target: str, distance_function, distance_name: str) -> pd.DataFrame:
        log("=================================================")
        # Start timing the classification process
        start_time = time()
        error = None
        # Calculate the distance
        try:
            value = distance_function.distance(clear(str(origin)), clear(str(target)))
        except Exception as e:
            error = f'Error calculating the distance: {e}'
            log(error)
            value = -1
        result = "HIGH" if value > self.impact_threshold else "ERROR" if value == -1 else "LOW"
        # End timing the classification process
        end_time = time()
        elapsed_time_ms = int((end_time - start_time) * 1000)
        # Log the classification result
        row = [result, f'{distance_name} distance [{self.impact_threshold}]: {value} {f"Error {error}" if error else ""}', elapsed_time_ms]
        log(f'Origin: [[{origin}]]\nTarget: [[{target}]]\nClassification result: {row}')
        # Append the classification result to the DataFrame
        self.df.loc[len(self.df)] = row
        return self.df

class LevenshteinClassifier(DistanceFunctionClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__(impact_threshold)
        self.levenshtein = NormalizedLevenshtein()

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        return self.classify_with_distance_function(origin, target, self.levenshtein, 'Levenshtein')

class NGramClassifier(DistanceFunctionClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__(impact_threshold)
        self.ngram = NGram(2)

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        return self.classify_with_distance_function(origin, target, self.ngram, 'N-Gram')

class JaccardClassifier(DistanceFunctionClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__(impact_threshold)
        self.jaccard = Jaccard(2)

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        return self.classify_with_distance_function(origin, target, self.jaccard, 'Jaccard')

class CosineClassifier(DistanceFunctionClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__(impact_threshold)
        self.cosine = Cosine(2)

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        return self.classify_with_distance_function(origin, target, self.cosine, 'Cosine')

class SorensenDiceClassifier(DistanceFunctionClassifierStrategy):
    def __init__(self, impact_threshold: float):
        super().__init__(impact_threshold)
        self.sorensenDice = SorensenDice(2)

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        return self.classify_with_distance_function(origin, target, self.sorensenDice, 'SorensenDice')