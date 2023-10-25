from utils import diferenca_primeiros_numeros
import pandas as pd

input_file = '2023_ICST_SRL_102023 - UNIFICADO - compendex.csv'
output_file = '2023_ICST_SRL_102023-UNIFICADO-COMPEDEX_out.csv'


df = pd.read_csv(input_file)
# Para cada linha, realize a atualização do número de páginas
for index, row in df.iterrows():
    print(f"{index}) {df.at[index, 'pages']}")
    diff = diferenca_primeiros_numeros(df.at[index, 'pages'])
    print(">>>>", diff)
    df.at[index, 'pages'] = diff
# Salvar o dataframe modificado como CSV
df.to_csv(output_file, index=False)
