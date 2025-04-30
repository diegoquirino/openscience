from langchain_core.language_models import BaseLanguageModel

from strategy import EditClassifierStrategy


class LLMClassifierStrategy(EditClassifierStrategy):
    def __init__(self, llm):
        super().__init__()
        self.llm = llm