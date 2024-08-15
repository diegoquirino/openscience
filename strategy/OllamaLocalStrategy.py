import data.utils as utils
import pandas as pd
import requests
import time
import os

from strategy import LLMStrategy


class OllamaLocalStrategy(LLMStrategy):
    """
    A concrete strategy implementation using Local Ollama for change classification.
    Docs: https://ollama.com/library
    """

    def __init__(self,
                 model: str,
                 url='http://localhost:11434/api/generate',
                 prompt_file=f'{os.path.join(os.getcwd(), 'data', 'prompts', 'DefaultPromptText.md')}',
                 rate_limit_per_round=0):
        # Saving desired data
        self.url = url
        self.model = model
        with open(prompt_file, 'r', encoding='utf-8') as f:
            self.prompt = f.read()
        self.df = pd.DataFrame(columns=LLMStrategy.OUTPUT_DF_COLUMN_NAMES)
        self.rate_limit_per_round = rate_limit_per_round

    def classify_change(self, origin: str, target: str):
        # Record the start time
        start_time = time.time()
        # Replace prompt origin and target use case excerpt
        prompt_temp = self.prompt.replace('[origin]', str(origin)).replace('[target]', str(target))
        # Asks Llama Text Generation an answer to the prompt
        data = {
            'model': f'{self.model}',
            'prompt': f'{prompt_temp}',
            'stream': False
        }
        headers = {'Content-Type': 'application/json'}
        response = requests.post(self.url, json=data, headers=headers)
        # Record the end time
        end_time = time.time()
        # Add answer to dataframe
        content_dict = utils.extract_json_answer(response.json()['response'])
        content_dict['elapsed_time_ms'] = int((end_time - start_time) * 1000)
        self.df.loc[len(self.df)] = content_dict
        print(content_dict)
        # Wait total 'rate_limit_per_round' secs to prevent API rate limit error
        time.sleep(self.rate_limit_per_round)
        return self.df
