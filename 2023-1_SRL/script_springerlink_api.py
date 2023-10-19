from utils import load_api_key, process_csv
import requests


def get_data_from_springer(doi):
    url = f"http://api.springernature.com/metadata/json/doi/{doi}?api_key={load_api_key()}"
    response = requests.get(url)
    data = response.json()
    # Obtenha o abstract
    abstract = data.get('records', [{}])[0].get('abstract', None)
    # Obtenha o pageLength
    page_length = data.get('result', [{}])[0].get('pageLength', None)
    # Obtenha a lista de keywords e converta-a em uma string separada por vírgulas
    keywords_list = [facet.get('values') for facet in data.get('facets', []) if facet['name'] == 'keyword'][0]
    keywords_list = [keyword['value'] for keyword in keywords_list]
    keywords = ", ".join(keywords_list)
    # Retorne o dicionário com abstract, page_lenght e keywords
    return {
        'Abstract': abstract,
        'PageLength': page_length,
        'Keywords': keywords
    }


# Exemplo de uso
input_file = '2023_ICST_SRL_102023 - SpringerLink.csv'
output_file = '2023_ICST_SRL_102023-SpringerLink_out.csv'
process_csv(input_file, output_file, 'Item DOI', get_data_from_springer)
