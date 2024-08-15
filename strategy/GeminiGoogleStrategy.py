import google.generativeai as genai
import data.utils as utils
import pandas as pd
import time
import os

from strategy import LLMStrategy


class GeminiGoogleStrategy(LLMStrategy):
    """
    A concrete strategy implementation using Google Gemini for change classification.
    """

    def __init__(self,
                 model: str,
                 prompt_file=f'{os.path.join(os.getcwd(), 'data', 'prompts', 'DefaultPromptText.md')}',
                 rate_limit_per_round=10):
        # Initializing OpenAI client
        genai.configure(api_key=os.getenv('GOOGLEAI_API_KEY'))
        generation_config = genai.GenerationConfig(
            temperature=1,
            max_output_tokens=8192,
            response_mime_type='application/json'
        )
        self.model = genai.GenerativeModel(
            model_name=f'{model}',
            generation_config=generation_config
        )
        with open(prompt_file, 'r', encoding='utf-8') as f:
            self.prompt = f.read()
        self.df = pd.DataFrame(columns=LLMStrategy.OUTPUT_DF_COLUMN_NAMES)
        self.rate_limit_per_round = rate_limit_per_round

    def classify_change(self, origin: str, target: str) -> pd.DataFrame:
        # Record the start time
        start_time = time.time()
        # Replace prompt origin and target use case excerpt
        prompt_temp = self.prompt.replace('[origin]', str(origin)).replace('[target]', str(target))
        # Asks ChatGPT Completions API an answer to the prompt
        response = self.model.start_chat().send_message(prompt_temp)
        # Record the end time
        end_time = time.time()
        # Add answer to dataframe
        content_dict = utils.extract_json_answer(response.text)
        content_dict['elapsed_time_ms'] = int((end_time - start_time) * 1000)
        self.df.loc[len(self.df)] = content_dict
        print(content_dict)
        # Wait total 'rate_limit_per_round' secs to prevent API rate limit error
        time.sleep(self.rate_limit_per_round)
        return self.df
