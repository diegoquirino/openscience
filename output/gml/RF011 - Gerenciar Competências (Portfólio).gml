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
  label "[c] Líder de Pessoas está autenticado no sistema e  tem permissão para gerenciar competências (portfólio)."
]
edge
[
  source 2
  target 3
  label "[s] Líder de Pessoas acessa a funcionalidade de gestão de competências (portfólio) a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem dos competências (portfólio) cadastrados com opções de 'Novo', 'Editar', 'Excluir' e 'Ajuda'"
]
edge
[
  source 4
  target 5
  label "[s] Líder de Pessoas clica na opção 'Novo' para criar um nova Competência (portfólio)"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulário para cadastro e alteração de competêcias (portfólio)"
]
edge
[
  source 6
  target 7
  label "[s] Líder de Pessoas seleciona no campo 'Tipo de Competência' a opção comportamental"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Tipo de Competência' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Líder de Pessoas preenche o campo 'Nome' com o nome da competência"
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
  label "[s] Líder de Pessoas preenche o campo 'Descrição' com a descrição da competência"
]
edge
[
  source 11
  target 12
  label "[e] system apresenta o campo 'Descrição' preenchido corretamente"
]
edge
[
  source 12
  target 13
  label "[s] Líder de Pessoas indica Não no campo 'Níveis estão modificados para esta competência'"
]
edge
[
  source 13
  target 14
  label "[e] system apresenta o campo 'Níveis estão modificados para esta competência' preenchido corretamente"
]
edge
[
  source 14
  target 15
  label "[s] Líder de Pessoas clica na opção 'Salvar'"
]
edge
[
  source 15
  target 16
  label "[e] system realiza a ação conforme a opção selecionada pelo usuário e     retorna feedback correspondente"
]
edge
[
  source 16
  target 17
  label "[c] A gestão de competências (portfólio) é realizada com sucesso."
]
edge
[
  source 2
  target 18
  label "[s] Usuario Não-Autenticado acessa a funcionalidade de gestão de competências (portfólio) a partir do menu inicial"
]
edge
[
  source 18
  target 17
  label "[e] system exibe a listagem dos competências (portfólio) cadastrados apenas para visualização com a opção 'Ajuda'"
]
edge
[
  source 4
  target 19
  label "[s] Líder de Pessoas seleciona um competêcias (portfólio) da listagem"
]
edge
[
  source 19
  target 20
  label "[e] system destaca o competêcias (portfólio) selecionado na listagem"
]
edge
[
  source 20
  target 5
  label "[s] Líder de Pessoas clica na opção 'Editar' para modificar o competêcias (portfólio) selecionado"
]
edge
[
  source 4
  target 21
  label "[s] Líder de Pessoas seleciona um competêcias (portfólio) da listagem"
]
edge
[
  source 21
  target 22
  label "[e] system destaca o competêcias (portfólio) selecionado na listagem"
]
edge
[
  source 22
  target 23
  label "[s] Líder de Pessoas clica na opção 'Excluir' para excluir o competêcias (portfólio) selecionado"
]
edge
[
  source 23
  target 24
  label "[e] system solicita confirmação de exclusão mostrando o nome da Competência (portfólio)"
]
edge
[
  source 24
  target 25
  label "[s] Líder de Pessoas confirma a exclusão da Competência (portfólio)"
]
edge
[
  source 25
  target 17
  label "[e] system exibe a listagem dos competências (portfólio) sem o competêcias (portfólio) excluído"
]
edge
[
  source 4
  target 26
  label "[s] Líder de Pessoas seleciona um competêcias (portfólio) da listagem"
]
edge
[
  source 26
  target 27
  label "[e] system destaca o competêcias (portfólio) selecionado na listagem"
]
edge
[
  source 27
  target 28
  label "[s] Líder de Pessoas clica na opção 'Excluir' para excluir o competêcias (portfólio) selecionado"
]
edge
[
  source 28
  target 29
  label "[e] system solicita confirmação de exclusão mostrando o nome da Competência (portfólio)"
]
edge
[
  source 29
  target 30
  label "[s] Líder de Pessoas não confirma a exclusão da Competência (portfólio)"
]
edge
[
  source 30
  target 17
  label "[e] system exibe a listagem dos competências (portfólio) com o competêcias (portfólio) excluído"
]
edge
[
  source 6
  target 7
  label "[s] Líder de Pessoas seleciona no campo 'Tipo de Competência' a opção técnica"
]
edge
[
  source 12
  target 31
  label "[s] Líder de Pessoas indica Sim no campo 'Níveis estão modificados para esta competência'"
]
edge
[
  source 31
  target 32
  label "[e] system apresenta o campo 'Níveis estão modificados para esta competência' preenchido corretamente"
]
edge
[
  source 32
  target 33
  label "[s] Líder de Pessoas preenche os dados na tabela do campo 'Níveis da Competência' com os novos nome, valor e descrição dos níveis de competência"
]
edge
[
  source 33
  target 34
  label "[e] system apresenta a tabela no campo 'Níveis da Competência' preenchida corretamente"
]
edge
[
  source 34
  target 15
  label "[s] Líder de Pessoas clica na opção 'Salvar'"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar salvar a nova Competência (portfólio), informando o campo ou a validação que falhou"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar editar a Competência (portfólio), informando o campo ou a validação que falhou"
]
edge
[
  source 15
  target 17
  label "[e] system exibe uma mensagem de erro ao tentar excluir a Competência (portfólio)"
]
]
