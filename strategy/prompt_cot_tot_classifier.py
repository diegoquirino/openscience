import os

import pandas as pd
from langchain_core.prompts import PromptTemplate
from strategy import LLMClassifierStrategy
from data.utils import log, extract_json_answer
from time import time, sleep


class PromptCoTToTClassifier(LLMClassifierStrategy):
    def __init__(self, llm, prompt_path):
        super().__init__(llm)
        if not prompt_path:
            raise ValueError('Prompt path is required.')
        self.prompt_path = os.path.normpath(prompt_path)

    def classify(self, origin: str, target: str) -> pd.DataFrame:
        log("=================================================")
        # Start timing the classification process
        start_time = time()
        with open(self.prompt_path, 'r', encoding='utf-8') as f:
            prompt = f.read()

        content_dict = {}
        # Retry mechanism
        for attempt in range(3):
            prompt_template = PromptTemplate(
                input_variables=["origin", "target"],
                template=prompt
            )
            chain = prompt_template | self.llm
            response = chain.invoke({"origin": origin, "target": target})
            # End timing the classification process
            end_time = time()
            elapsed_time_ms = int((end_time - start_time) * 1000)
            # Log the classification result
            content_dict = extract_json_answer(response.content)
            content_dict['elapsed_time_ms'] = elapsed_time_ms
            log(f'Origin: [[{origin}]]\nTarget: [[{target}]]\nComplete Response: [[{response}]]\nClassification result: {content_dict}')
            if content_dict.get('edit_classification') in ['HIGH', 'LOW']:
                break
            sleep(30)
            log(f'Attempt {attempt + 1} failed to get valid CoTToT edit classification. Retrying...')

        # Append the classification result to the DataFrame
        self.df.loc[len(self.df)] = content_dict
        return self.df