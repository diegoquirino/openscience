import os
import unidecode

def remove_accents_from_text(text):
    # Remover acentos do texto usando unidecode
    return unidecode.unidecode(text)

def remove_accents_from_file(file_path):
    # Abrir o arquivo .claret e ler seu conteúdo
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # Remover acentos do conteúdo
    content_without_accents = remove_accents_from_text(content)
    
    # Sobrescrever o arquivo com o conteúdo sem acentos
    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(content_without_accents)

def rename_file_without_accents(old_path):
    # Extrair o diretório e o nome do arquivo
    directory, filename = os.path.split(old_path)
    
    # Remover acentos do nome do arquivo
    filename_without_accents = remove_accents_from_text(filename)
    
    # Criar o novo caminho para o arquivo
    new_path = os.path.join(directory, filename_without_accents)
    
    # Renomear o arquivo
    os.rename(old_path, new_path)
    
    return new_path

def process_claret_files(directory):
    # Listar todos os arquivos no diretório fornecido
    for filename in os.listdir(directory):
        if filename.endswith(".claret"):
            old_file_path = os.path.join(directory, filename)
            print(f"Processando arquivo: {old_file_path}")
            
            # Remover acentos do conteúdo do arquivo
            remove_accents_from_file(old_file_path)
            
            # Renomear o arquivo removendo acentos do nome
            new_file_path = rename_file_without_accents(old_file_path)
            
            print(f"Arquivo renomeado: {new_file_path}")
    
    print("Processo concluído!")

if __name__ == "__main__":
    # Diretório onde os arquivos .claret estão localizados
    directory = os.getcwd()  # Obtém o diretório atual
    process_claret_files(directory)

