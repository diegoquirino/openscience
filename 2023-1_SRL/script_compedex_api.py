from utils import load_api_key, error, process_csv, diferenca_primeiros_numeros
import requests


def get_data_from_compendex(title="Provengo: A Tool Suite for Scenario Driven Model-Based Testing"):
    url = f"https://api.elsevier.com/content/ev/results"
    params = {
        "query": f"{title}",
        "apiKey": load_api_key()
    }
    return requests.get(url, params=params)
    # response = requests.get(url, params=params)
    # if response.status_code == 200:
    #     data = response.json()
    #     for entry in data['search-results']['entry']:
    #         page_length = diferenca_primeiros_numeros(entry.get('prism:pageRange', "N/A"))
    #         abstract = entry.get('dc:description', "N/A")
    #         keywords = entry.get('authkeywords', "N/A").replace(" | ", ", ")
    #         return {
    #             'Abstract': abstract,
    #             'PageLength': page_length,
    #             'Keywords': keywords
    #         }
    # else:
    #     error(f"Erro ao obter os dados do artigo '{title}' :: erro {response.status_code} - {response.reason}")
    #     return {
    #         'Abstract': '',
    #         'PageLength': '',
    #         'Keywords': ''
    #     }


# Exemplo de uso
input_file = '2023_ICST_SRL_102023 - Compendex.csv'
output_file = '2023_ICST_SRL_102023-Compendex_out.csv'
#process_csv(input_file, output_file, 'Title', get_data_from_scopus)
print(get_data_from_compendex())
