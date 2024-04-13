from utils import load_api_key, load_inst_token, error, process_csv, diferenca_primeiros_numeros
import requests


def get_data_from_compendex(title="TEST CASE"):
    url = f"https://api.elsevier.com/content/ev/results"
    headers = {
        "X-ELS-APIKey": load_api_key(),
        "X-ELS-InstToken": load_inst_token()
    }
    params = {
        "query": f"{title}"
    }
    response = requests.get(url, headers=headers, params=params)
    if response.status_code == 200:
        return {
            'Abstract': '',
            'PageLength': '',
            'Keywords': '',
            'data': response.json()
        }
        # data = response.json()
        # for entry in data['search-results']['entry']:
        #     page_length = diferenca_primeiros_numeros(entry.get('prism:pageRange', "N/A"))
        #     abstract = entry.get('dc:description', "N/A")
        #     keywords = entry.get('authkeywords', "N/A").replace(" | ", ", ")
        #     return {
        #         'Abstract': abstract,
        #         'PageLength': page_length,
        #         'Keywords': keywords
        #     }
    else:
        error(f"Erro ao obter os dados do artigo '{title}' :: erro {response.status_code} - {response.reason}")
        return {
            'Abstract': '',
            'PageLength': '',
            'Keywords': ''
        }


# Exemplo de uso
input_file = '2023_ICST_SRL_102023 - Compendex.csv'
output_file = '2023_ICST_SRL_102023- Compendex_out.csv'
#process_csv(input_file, output_file, 'Title', get_data_from_compendex)
print(get_data_from_compendex())
