import pandas as pd
import time
import json
import os
import re
import requests
import google.generativeai as genai
from openai import OpenAI
from abc import ABC, abstractmethod


OUTPUT_DF_COLUMN_NAMES = ['edit_classification', 'decision_rationale', 'elapsed_time_ms']


def extract_json_answer(input_string):
    # Using a regular expression to extract the JSON from the string
    match = re.search(r'\{.*?}', input_string, re.DOTALL)
    if match:
        json_str = match.group()
        try:
            json_data = json.loads(json_str)
            return json_data
        except json.JSONDecodeError as e:
            print(f"Erro ao decodificar JSON: {e}")
            return None
    else:
        print("Nenhum JSON encontrado na string.")
        return None


class LLMStrategy(ABC):
    @abstractmethod
    def classify_change(self, origin, target):
        pass


class ChatGPTOpenAiStrategy(LLMStrategy):

    def __init__(self,
                 model: str,
                 prompt_file=f'{os.path.join(os.getcwd(), 'data', 'DefaultPromptText.md')}',
                 rate_limit_per_round=10):
        # Initializing OpenAI client
        self.client = OpenAI(api_key=os.getenv('OPENAI_API_KEY'))
        self.model = model
        with open(prompt_file, 'r', encoding='utf-8') as f:
            self.prompt = f.read()
        self.df = pd.DataFrame(columns=OUTPUT_DF_COLUMN_NAMES)
        self.rate_limit_per_round = rate_limit_per_round

    def classify_change(self, origin: str, target: str):
        # Record the start time
        start_time = time.time()
        # Replace prompt origin and target use case excerpt
        prompt_temp = self.prompt.replace('[origin]', str(origin)).replace('[target]', str(target))
        # Asks ChatGPT Completions API an answer to the prompt
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {
                    'role': 'user',
                    'content': f'{prompt_temp}'
                }
            ],
            temperature=1,
            max_tokens=1024,
            top_p=1,
            frequency_penalty=0,
            presence_penalty=0
        )
        # Record the end time
        end_time = time.time()
        # Add answer to dataframe
        content_dict = extract_json_answer(response.choices[0].message.content)
        content_dict['elapsed_time_ms'] = int((end_time - start_time) * 1000)
        self.df.loc[len(self.df)] = content_dict
        print(content_dict)
        # Wait total 'rate_limit_per_round' secs to prevent API rate limit error
        time.sleep(self.rate_limit_per_round)
        return self.df


class OllamaStrategy(LLMStrategy):

    def __init__(self,
                 model: str,
                 url='http://localhost:11434/api/generate',
                 prompt_file=f'{os.path.join(os.getcwd(), 'data', 'DefaultPromptText.md')}',
                 rate_limit_per_round=0):
        # Saving desired data
        self.url = url
        self.model = model
        with open(prompt_file, 'r', encoding='utf-8') as f:
            self.prompt = f.read()
        self.df = pd.DataFrame(columns=OUTPUT_DF_COLUMN_NAMES)
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
        content_dict = extract_json_answer(response.json()['response'])
        content_dict['elapsed_time_ms'] = int((end_time - start_time) * 1000)
        self.df.loc[len(self.df)] = content_dict
        print(content_dict)
        # Wait total 'rate_limit_per_round' secs to prevent API rate limit error
        time.sleep(self.rate_limit_per_round)
        return self.df


class GoogleGeminiStrategy(LLMStrategy):

    def __init__(self,
                 model: str,
                 prompt_file=f'{os.path.join(os.getcwd(), 'data', 'DefaultPromptText.md')}',
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
        self.df = pd.DataFrame(columns=OUTPUT_DF_COLUMN_NAMES)
        self.rate_limit_per_round = rate_limit_per_round

    def classify_change(self, origin: str, target: str):
        # Record the start time
        start_time = time.time()
        # Replace prompt origin and target use case excerpt
        prompt_temp = self.prompt.replace('[origin]', str(origin)).replace('[target]', str(target))
        # Asks ChatGPT Completions API an answer to the prompt
        response = self.model.start_chat().send_message(prompt_temp)
        # Record the end time
        end_time = time.time()
        # Add answer to dataframe
        content_dict = extract_json_answer(response.text)
        content_dict['elapsed_time_ms'] = int((end_time - start_time) * 1000)
        self.df.loc[len(self.df)] = content_dict
        print(content_dict)
        # Wait total 'rate_limit_per_round' secs to prevent API rate limit error
        time.sleep(self.rate_limit_per_round)
        return self.df
