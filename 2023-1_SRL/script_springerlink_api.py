import pandas as pd
import requests
import time


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


def process_csv(input_file, output_file):
    df = pd.read_csv(input_file)

    # Para cada DOI, obtenha os dados e adicione ao dataframe
    for index, row in df.iterrows():
        try:
            data = get_data_from_springer(row['Item DOI'])
            df.at[index, 'Keywords'] = data['Keywords']
            df.at[index, 'Pages'] = data['PageLength']
            df.at[index, 'Abstract'] = data['Abstract']
            print(f"{index}. {data}")  # Imprimir o JSON retornado
            if index != len(df) - 1:  # Não esperar após a última solicitação
                time.sleep(10)  # Espera 10 segundos
        except:
            error(f"Erro ao obter os dados do artigo '{row['Item Title']}' DOI '{row['Item DOI']}'")

    # Salvar o dataframe atualizado como CSV
    df.to_csv(output_file, index=False)


def load_api_key(filename="apikey"):
    with open(filename, 'r') as file:
        return file.readline().strip()


def error(message):
    print(message)
    with open("error.log", "a") as log_file:
        log_file.write(message)


# Exemplo de uso
input_file = '2023_ICST_SRL_102023 - SpringerLink.csv'
output_file = '2023_ICST_SRL_102023-SpringerLink_out.csv'
process_csv(input_file, output_file)
