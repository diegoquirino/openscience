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
node
[
  id 16
  label "16"
]
node
[
  id 17
  label "17"
]
node
[
  id 18
  label "18"
]
node
[
  id 19
  label "19"
]
node
[
  id 20
  label "20"
]
node
[
  id 21
  label "21"
]
node
[
  id 22
  label "22"
]
node
[
  id 23
  label "23"
]
edge
[
  source 1
  target 2
  label "[c] Avaliador de Pessoas esta autenticado no sistema e tem permissao para gerenciar Respostas de Avaliacoes"
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
  label "[e] system exibe a listagem das Avaliacoes cadastradas com a opcao 'Formulario' dentre as varias exibidas"
]
edge
[
  source 4
  target 5
  label "[s] Avaliador de Pessoas com uma avaliacao selecionada, clica na opcao 'Formulario' para responder a uma Avaliacao ou Autoavaliacao"
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
  label "[s] Avaliador de Pessoas seleciona o 'Nivel de Interacao' para cada Perfil de Competencias avaliado"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Nivel de Interacao' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Avaliador de Pessoas seleciona que e 'Capaz de Avaliar a Competencia' para cada Perfil de Competencias avaliado"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta o campo 'Capaz de Avaliar' preenchido como 'SIM'"
]
edge
[
  source 10
  target 11
  label "[s] Avaliador de Pessoas seleciona o 'Nivel da Competencia' da Avaliacao (1 a 4) para cada Perfil de Competencias avaliado"
]
edge
[
  source 11
  target 12
  label "[e] system apresenta o campo 'Nivel da Competencia' preenchido corretamente"
]
edge
[
  source 12
  target 13
  label "[s] Avaliador de Pessoas adiciona 'Apontamentos' sobre a avaliacao para cada Perfil de Competencias avaliado"
]
edge
[
  source 13
  target 14
  label "[e] system apresenta os 'Apontamentos' preenchidos corretamente"
]
edge
[
  source 14
  target 15
  label "[s] Avaliador de Pessoas clica na opcao 'Salvar'"
]
edge
[
  source 15
  target 16
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e      retorna feedback correspondente"
]
edge
[
  source 16
  target 17
  label "[c] A gestao de Respostas de Avaliacoes e realizada com sucesso."
]
edge
[
  source 5
  target 17
  label "[e] system exibe uma mensagem de erro informando ao usuario que nao e um dos avaliadores"
]
edge
[
  source 8
  target 18
  label "[s] Avaliador de Pessoas seleciona que nao e 'Capaz de Avaliar a Competencia' para cada Perfil de Competencias avaliado"
]
edge
[
  source 18
  target 19
  label "[e] system apresenta o campo 'Capaz de Avaliar' preenchido como 'NAO'"
]
edge
[
  source 19
  target 20
  label "[s] Avaliador de Pessoas preenche o campo de 'Justificativas' para cada Perfil de Competencias avaliado"
]
edge
[
  source 20
  target 21
  label "[e] system apresenta o campo de 'Capaz de Avaliar' preenchido corretamente"
]
edge
[
  source 21
  target 15
  label "[s] Avaliador de Pessoas clica no botao 'Justificar'"
]
edge
[
  source 14
  target 22
  label "[s] Avaliador de Pessoas clica na opcao 'Excluir' para cada Perfil de Competencias avaliado"
]
edge
[
  source 22
  target 23
  label "[e] system limpa os campos 'Nivel da Competencia' e 'Apontamentos' exibidos na tela para cada Perfil de Competencias avaliado"
]
edge
[
  source 23
  target 6
  label "[s] Avaliador de Pessoas verifica que os campos 'Nivel da Competencia' e 'Apontamentos' estao limpos"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar salvar, informando o campo ou a validacao que falhou"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar editar, informando o campo ou a validacao que falhou"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro informando ao usuario que nao informou justificativa"
]
]
