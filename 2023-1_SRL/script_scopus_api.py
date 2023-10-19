from utils import load_api_key, error, process_csv, diferenca_primeiros_numeros
import requests


def get_data_from_scopus(title="Model-Based Testing of Smart Home Systems Using EFSM and CEFSM"):
    url = f"https://api.elsevier.com/content/search/scopus"
    params = {
        "query": f"TITLE({title})",
        "field": "title,description,authkeywords,pageRange",
        "view": "COMPLETE",
        "apiKey": load_api_key()
    }
    response = requests.get(url, params=params)
    if response.status_code == 200:
        data = response.json()
        for entry in data['search-results']['entry']:
            page_length = diferenca_primeiros_numeros(entry.get('prism:pageRange', "N/A"))
            abstract = entry.get('dc:description', "N/A")
            keywords = entry.get('authkeywords', "N/A").replace(" | ", ", ")
            return {
                'Abstract': abstract,
                'PageLength': page_length,
                'Keywords': keywords
            }
    else:
        error(f"Erro ao obter os dados do artigo '{title}' :: erro {response.status_code} - {response.reason}")


# Exemplo de uso
input_file = '2023_ICST_SRL_102023 - Scopus.csv'
output_file = '2023_ICST_SRL_102023-Scopus_out.csv'
process_csv(input_file, output_file, 'Title', get_data_from_scopus)
