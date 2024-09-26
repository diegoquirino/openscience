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
node
[
  id 15
  label "15"
]
edge
[
  source 1
  target 2
  label "[c] Avaliador de Pessoas esta autenticado no sistema; e, tem permissao para gerenciar Respostas de Avaliacoes"
]
edge
[
  source 2
  target 3
  label "[s] Avaliador de Pessoas acessa a funcionalidade de Avaliacoes a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem das Avaliacoes cadastradas com a opcao 'Formulario' dentre as varias listadas"
]
edge
[
  source 4
  target 5
  label "[s] Avaliador de Pessoas com uma avaliacao selecionada, clica na opcao 'Formulario' para criar uma nova Avaliacao"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para cadastro e alteracao de Respostas de Avaliacao"
]
edge
[
  source 6
  target 7
  label "[s] Avaliador de Pessoas seleciona o 'Nivel da Competencia' da Avaliacao (1 a 4) para cada Perfil de Competencias avaliado"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Nivel da Competencia' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Avaliador de Pessoas adiciona 'Apontamentos' sobre a avaliacao para cada Perfil de Competencias avaliado"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta os 'Apontamentos' preenchidos corretamente"
]
edge
[
  source 10
  target 11
  label "[s] Avaliador de Pessoas clica na opcao 'Salvar'"
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
  label "[c] A gestao de Respostas de Avaliacoes e realizada com sucesso."
]
edge
[
  source 5
  target 13
  label "[e] system exibe uma mensagem de erro informando ao usuario que nao e um dos avaliadores"
]
edge
[
  source 10
  target 14
  label "[s] Avaliador de Pessoas clica na opcao 'Excluir' para cada Perfil de Competencias avaliado"
]
edge
[
  source 14
  target 15
  label "[e] system limpa os campos apresentados 'Nivel da Competencia' e 'Apontamentos' apresentados na tela para cada Perfil de Competencias avaliado"
]
edge
[
  source 15
  target 6
  label "[s] Avaliador de Pessoas verifica que os campos 'Nivel da Competencia' e 'Apontamentos' estao limpos"
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
