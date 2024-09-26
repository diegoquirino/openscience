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
node
[
  id 24
  label "24"
]
node
[
  id 25
  label "25"
]
node
[
  id 26
  label "26"
]
node
[
  id 27
  label "27"
]
node
[
  id 28
  label "28"
]
node
[
  id 29
  label "29"
]
node
[
  id 30
  label "30"
]
node
[
  id 31
  label "31"
]
node
[
  id 32
  label "32"
]
node
[
  id 33
  label "33"
]
node
[
  id 34
  label "34"
]
edge
[
  source 1
  target 2
  label "[c] Lider de Pessoas esta autenticado no sistema e  tem permissao para gerenciar Competencias (Portfolio)."
]
edge
[
  source 2
  target 3
  label "[s] Lider de Pessoas acessa a funcionalidade de gestao de Competencias (Portfolio) a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem dos Competencias (Portfolio) cadastrados com opcoes de 'Novo', 'Editar', 'Excluir' e 'Ajuda'"
]
edge
[
  source 4
  target 5
  label "[s] Lider de Pessoas clica na opcao 'Novo' para criar um nova Competencia (portfolio)"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para cadastro e alteracao de Competencias (Portfolio)"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas seleciona no campo 'Tipo de Competencia' a opcao comportamental"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Tipo de Competencia' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Lider de Pessoas preenche o campo 'Nome' com o nome da competencia"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta o campo 'Nome' preenchido corretamente"
]
edge
[
  source 10
  target 11
  label "[s] Lider de Pessoas preenche o campo 'Descricao' com a descricao da competencia"
]
edge
[
  source 11
  target 12
  label "[e] system apresenta o campo 'Descricao' preenchido corretamente"
]
edge
[
  source 12
  target 13
  label "[s] Lider de Pessoas indica Nao no campo 'Niveis estao modificados para esta competencia'"
]
edge
[
  source 13
  target 14
  label "[e] system apresenta o campo 'Niveis estao modificados para esta competencia' preenchido corretamente"
]
edge
[
  source 14
  target 15
  label "[s] Lider de Pessoas clica na opcao 'Salvar'"
]
edge
[
  source 15
  target 16
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e     retorna feedback correspondente"
]
edge
[
  source 16
  target 17
  label "[c] A gestao de Competencias (Portfolio) e realizada com sucesso."
]
edge
[
  source 2
  target 18
  label "[s] Usuario Nao-Autenticado acessa a funcionalidade de gestao de Competencias (Portfolio) a partir do menu inicial"
]
edge
[
  source 18
  target 17
  label "[e] system exibe a listagem dos Competencias (Portfolio) cadastrados apenas para visualizacao com a opcao 'Ajuda'"
]
edge
[
  source 4
  target 19
  label "[s] Lider de Pessoas seleciona um Competencias (Portfolio) da listagem"
]
edge
[
  source 19
  target 20
  label "[e] system destaca o Competencias (Portfolio) selecionado na listagem"
]
edge
[
  source 20
  target 5
  label "[s] Lider de Pessoas clica na opcao 'Editar' para modificar o Competencias (Portfolio) selecionado"
]
edge
[
  source 4
  target 21
  label "[s] Lider de Pessoas seleciona um Competencias (Portfolio) da listagem"
]
edge
[
  source 21
  target 22
  label "[e] system destaca o Competencias (Portfolio) selecionado na listagem"
]
edge
[
  source 22
  target 23
  label "[s] Lider de Pessoas clica na opcao 'Excluir' para excluir o Competencias (Portfolio) selecionado"
]
edge
[
  source 23
  target 24
  label "[e] system solicita confirmacao de exclusao mostrando o nome da Competencia (portfolio)"
]
edge
[
  source 24
  target 25
  label "[s] Lider de Pessoas confirma a exclusao da Competencia (portfolio)"
]
edge
[
  source 25
  target 17
  label "[e] system exibe a listagem dos Competencias (Portfolio) sem o Competencias (Portfolio) excluido"
]
edge
[
  source 4
  target 26
  label "[s] Lider de Pessoas seleciona um Competencias (Portfolio) da listagem"
]
edge
[
  source 26
  target 27
  label "[e] system destaca o Competencias (Portfolio) selecionado na listagem"
]
edge
[
  source 27
  target 28
  label "[s] Lider de Pessoas clica na opcao 'Excluir' para excluir o Competencias (Portfolio) selecionado"
]
edge
[
  source 28
  target 29
  label "[e] system solicita confirmacao de exclusao mostrando o nome da Competencia (portfolio)"
]
edge
[
  source 29
  target 30
  label "[s] Lider de Pessoas nao confirma a exclusao da Competencia (portfolio)"
]
edge
[
  source 30
  target 17
  label "[e] system exibe a listagem dos Competencias (Portfolio) com o Competencias (Portfolio) excluido"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas seleciona no campo 'Tipo de Competencia' a opcao tecnica"
]
edge
[
  source 12
  target 31
  label "[s] Lider de Pessoas indica Sim no campo 'Niveis estao modificados para esta competencia'"
]
edge
[
  source 31
  target 32
  label "[e] system apresenta o campo 'Niveis estao modificados para esta competencia' preenchido corretamente"
]
edge
[
  source 32
  target 33
  label "[s] Lider de Pessoas preenche os dados na tabela do campo 'Niveis da Competencia' com os novos nome, valor e descricao dos niveis de competencia"
]
edge
[
  source 33
  target 34
  label "[e] system apresenta a tabela no campo 'Niveis da Competencia' preenchida corretamente"
]
edge
[
  source 34
  target 15
  label "[s] Lider de Pessoas clica na opcao 'Salvar'"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar salvar a nova Competencia (portfolio), informando o campo ou a validacao que falhou"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar editar a Competencia (portfolio), informando o campo ou a validacao que falhou"
]
edge
[
  source 25
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar excluir a Competencia (portfolio)"
]
]
