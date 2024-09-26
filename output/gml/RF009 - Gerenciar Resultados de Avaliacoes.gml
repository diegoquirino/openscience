graph
[
node
[
  id 1
  label "1"
]
node
[
  id 2
  label "2"
]
node
[
  id 3
  label "3"
]
node
[
  id 4
  label "4"
]
node
[
  id 5
  label "5"
]
node
[
  id 6
  label "6"
]
node
[
  id 7
  label "7"
]
node
[
  id 8
  label "8"
]
node
[
  id 9
  label "9"
]
node
[
  id 10
  label "10"
]
node
[
  id 11
  label "11"
]
node
[
  id 12
  label "12"
]
node
[
  id 13
  label "13"
]
node
[
  id 14
  label "14"
]
edge
[
  source 1
  target 2
  label "[c] Lider de Pessoas esta autenticado no sistema; e, tem permissao para gerenciar Resultados de Avaliacoes"
]
edge
[
  source 2
  target 3
  label "[s] Lider de Pessoas acessa a funcionalidade de Avaliacoes a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem das Avaliacoes cadastradas com a opcao 'Resultado' dentre as varias exibidas"
]
edge
[
  source 4
  target 5
  label "[s] Lider de Pessoas com uma avaliacao selecionada, clica na opcao 'Resultado' para efetivar o feedback"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para cadastro da Ata de Reuniao"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas preenche a 'Ata de Reuniao de Feedback' da Avaliacao para cada Perfil de Competencias avaliado"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Ata de Reuniao de Feedback' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Lider de Pessoas verifica a presenca (somente leitura) do detalhe de cada competencia avaliada"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta os detalhes de cada competencia, incluindo:                    - a descricao da competencia;                   - o valor da autoavaliacao, a media das notas dos avaliadores e o valor da meta do avaliado;                   - os niveis de avaliacao para aquela competencia; e,                   - os apontamentos."
]
edge
[
  source 10
  target 11
  label "[s] Lider de Pessoas clica na opcao 'Salvar'"
]
edge
[
  source 11
  target 12
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e     retorna feedback correspondente"
]
edge
[
  source 12
  target 13
  label "[c] A gestao de Resultados de Avaliacoes e realizada com sucesso."
]
edge
[
  source 10
  target 14
  label "[s] Lider de Pessoas clica na opcao 'Encerrar' para a Avaliacao de Competencias"
]
edge
[
  source 14
  target 13
  label "[e] system apresenta o campo 'Ata de Reuniao de Feedback'"
]
edge
[
  source 11
  target 13
  label "[e] system exibe uma mensagem de erro ao tentar salvar, informando o campo ou a validacao que falhou"
]
edge
[
  source 11
  target 13
  label "[e] system exibe uma mensagem de erro ao tentar editar, informando o campo ou a validacao que falhou"
]
]
