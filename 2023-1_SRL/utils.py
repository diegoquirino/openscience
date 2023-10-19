import pandas as pd
import time
import re


def process_csv(input_file, output_file, search_column, fetch_data_function):
    df = pd.read_csv(input_file)
    # Para cada DOI, obtenha os dados e adicione ao dataframe
    for index, row in df.iterrows():
        try:
            search_text = get_first_part_of(row[search_column], [";", "["])
            data = fetch_data_function(search_text)
            df.at[index, 'Keywords'] = data['Keywords']
            df.at[index, 'Pages'] = data['PageLength']
            df.at[index, 'Abstract'] = data['Abstract']
            print(f"{index}. {data}")  # Imprimir o JSON retornado
            if index != len(df) - 1:  # Não esperar após a última solicitação
                time.sleep(10)  # Espera 10 segundos
        except:
            error(f"Erro ao obter os dados do artigo {search_column}='{row[search_column]}'")
    # Salvar o dataframe atualizado como CSV
    df.to_csv(output_file, index=False)


def diferenca_primeiros_numeros(s):
    # Extrai todos os números da string
    numeros = re.findall(r'(\d+)', s)
    # Se apenas um número foi encontrado, retorna o próprio número
    if len(numeros) == 1:
        return int(numeros[0])
    # Se pelo menos dois números foram encontrados, retorna a diferença +1
    elif len(numeros) >= 2:
        return abs(int(numeros[0]) - int(numeros[1])) + 1
    # Se nenhum número foi encontrado, retorna 0
    else:
        return 0


def error(message):
    print(message)
    with open("error.log", "a") as log_file:
        log_file.write(f"{message}\n")


def load_api_key(filename="apikey"):
    with open(filename, 'r') as file:
        return file.readline().strip()


def get_first_part_of(text, chars):
    for char in chars:
        text = text.split(char)[0]
    return text
