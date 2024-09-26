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
edge
[
  source 1
  target 2
  label "[c] Lider de Pessoas esta autenticado no sistema e  tem permissao para gerenciar Niveis das Competencias."
]
edge
[
  source 2
  target 3
  label "[s] Lider de Pessoas acessa a funcionalidade de gestao de Niveis das Competencias a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem dos Niveis das Competencias cadastrados com opcoes de 'Novo', 'Editar', 'Excluir' e 'Ajuda'"
]
edge
[
  source 4
  target 5
  label "[s] Lider de Pessoas clica na opcao 'Novo' para criar um novo Nivel das Competencias"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para cadastro e alteracao de Niveis das Competencias"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas preenche o campo 'Nome' com dados validos"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Nome' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Lider de Pessoas preenche o campo 'Valor' com um numero inteiro referente a pontuacao a ser obtida no atendimento do respectivo nivel"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta o campo 'Valor' preenchido corretamente"
]
edge
[
  source 10
  target 11
  label "[s] Lider de Pessoas preenche o campo 'Descricao' com a descricao do nivel de competencia"
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
  label "[s] Lider de Pessoas clica na opcao 'Salvar'"
]
edge
[
  source 13
  target 14
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e     retorna feedback correspondente"
]
edge
[
  source 14
  target 15
  label "[c] A gestao de Niveis das Competencias e realizada com sucesso."
]
edge
[
  source 2
  target 16
  label "[s] Usuario Nao-Autenticado acessa a funcionalidade de gestao de Niveis das Competencias a partir do menu inicial"
]
edge
[
  source 16
  target 15
  label "[e] system exibe a listagem dos Niveis das Competencias cadastrados apenas para visualizacao com a opcao 'Ajuda'"
]
edge
[
  source 4
  target 17
  label "[s] Lider de Pessoas seleciona um Nivel das Competencias da listagem"
]
edge
[
  source 17
  target 18
  label "[e] system destaca o Nivel das Competencias selecionado na listagem"
]
edge
[
  source 18
  target 5
  label "[s] Lider de Pessoas clica na opcao 'Editar' para modificar o Nivel das Competencias selecionado"
]
edge
[
  source 4
  target 19
  label "[s] Lider de Pessoas seleciona um Nivel das Competencias da listagem"
]
edge
[
  source 19
  target 20
  label "[e] system destaca o Nivel das Competencias selecionado na listagem"
]
edge
[
  source 20
  target 21
  label "[s] Lider de Pessoas clica na opcao 'Excluir' para excluir o Nivel das Competencias selecionado"
]
edge
[
  source 21
  target 22
  label "[e] system solicita confirmacao de exclusao"
]
edge
[
  source 22
  target 23
  label "[s] Lider de Pessoas confirma a exclusao do Nivel das Competencias"
]
edge
[
  source 23
  target 15
  label "[e] system exibe a listagem dos Niveis das Competencias sem o Nivel das Competencias excluido"
]
edge
[
  source 4
  target 24
  label "[s] Lider de Pessoas seleciona um Nivel das Competencias da listagem"
]
edge
[
  source 24
  target 25
  label "[e] system destaca o Nivel das Competencias selecionado na listagem"
]
edge
[
  source 25
  target 26
  label "[s] Lider de Pessoas clica na opcao 'Excluir' para excluir o Nivel das Competencias selecionado"
]
edge
[
  source 26
  target 27
  label "[e] system solicita confirmacao de exclusao"
]
edge
[
  source 27
  target 28
  label "[s] Lider de Pessoas nao confirma a exclusao do Nivel das Competencias"
]
edge
[
  source 28
  target 15
  label "[e] system exibe a listagem dos Niveis das Competencias com o Nivel das Competencias nao excluido"
]
edge
[
  source 13
  target 15
  label "[e] system exibe uma mensagem de erro ao tentar salvar o novo Nivel das Competencias, informando o campo ou a validacao que falhou"
]
edge
[
  source 13
  target 15
  label "[e] system exibe uma mensagem de erro ao tentar editar o Nivel das Competencias, informando o campo ou a validacao que falhou"
]
edge
[
  source 23
  target 15
  label "[e] system exibe uma mensagem de erro ao tentar excluir o Nivel das Competencias"
]
]
