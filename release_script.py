import subprocess
import requests
import sys
import configparser
import argparse


def load_config():
    """
    Função para carregar as configurações do arquivo github.conf.
    """
    config = configparser.ConfigParser()
    config.read('github.conf')

    if 'repository' not in config:
        print("Erro: Seção 'repository' não encontrada no arquivo github.conf.")
        sys.exit(1)

    return {
        'github_token': config['repository'].get('github_token'),
        'repo_owner': config['repository'].get('repo_owner'),
        'repo_name': config['repository'].get('repo_name'),
        'branch_name': config['repository'].get('branch_name'),
        'tag_prefix': config['repository'].get('tag_prefix'),
        'release_prefix': config['repository'].get('release_prefix'),
        'release_body_template': config['repository'].get('release_body_template')
    }


def run_command(command):
    """
    Função para executar comandos no terminal e capturar erros.
    """
    try:
        subprocess.run(command, check=True, shell=True, text=True)
    except subprocess.CalledProcessError as e:
        print(f"Erro ao executar o comando: {e.cmd}")
        sys.exit(1)


def create_tag(config, tag_name, release_name):
    """
    Função para criar e enviar a tag para o repositório remoto.
    """
    branch_name = config['branch_name']

    print(f"Fazendo checkout do branch {branch_name}...")
    run_command(f"git checkout {branch_name}")

    print("Atualizando o branch...")
    run_command(f"git pull origin {branch_name}")

    print(f"Criando a tag {tag_name}...")
    run_command(f'git tag -a {tag_name} -m "Tag for {release_name}"')

    print(f"Enviando a tag {tag_name} para o repositório remoto...")
    run_command(f"git push origin {tag_name}")


def create_release(config, tag_name, release_name, release_body):
    """
    Função para criar a release no GitHub via API.
    """
    url = f"https://api.github.com/repos/{config['repo_owner']}/{config['repo_name']}/releases"
    headers = {
        "Authorization": f"token {config['github_token']}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {
        "tag_name": tag_name,
        "target_commitish": config['branch_name'],
        "name": release_name,
        "body": release_body,
        "draft": False,
        "prerelease": False
    }

    print("Criando a release no GitHub...")
    response = requests.post(url, json=payload, headers=headers)

    if response.status_code == 201:
        print(f"Release {release_name} criada com sucesso.")
    else:
        print(f"Erro ao criar a release: {response.status_code}")
        print(response.json())
        sys.exit(1)


def main():
    # Argumentos da linha de comando
    parser = argparse.ArgumentParser(description='Criar tag e release no GitHub.')
    parser.add_argument('version', help='A versão para a tag e release (ex: v0.1)')
    args = parser.parse_args()

    # Carrega configurações
    config = load_config()

    # Define nomes para a tag e release com base na versão fornecida
    version = args.version
    tag_name = f"{config['tag_prefix']}_{version}"
    release_name = f"{config['release_prefix']} {version}"
    release_body = f"{config['release_body_template']} {version}"

    # Criação da tag e da release
    create_tag(config, tag_name, release_name)
    create_release(config, tag_name, release_name, release_body)


if __name__ == "__main__":
    main()
