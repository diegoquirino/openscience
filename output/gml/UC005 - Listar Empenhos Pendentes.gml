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
edge
[
  source 1
  target 2
  label "[c] O usuario devidamente autenticado e na tela de listagem de empenhos"
]
edge
[
  source 2
  target 3
  label "[s] Chefe/Beneficiário Clica para exibir a lista de solicitações aguardando empenho."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe a lista de solicitações aguardando serem empenhadas."
]
edge
[
  source 4
  target 5
  label "[c] O caso de uso encerra."
]
edge
[
  source 2
  target 6
  label "[s] Chefe/Beneficiário Clica para que o sistema exiba detalhes da solicitação da diária."
]
edge
[
  source 6
  target 5
  label "[e] system Recupera e exibe todos os detalhes (dados) da solicitação para o usuário; e Apresenta a tela de Detalhar Diárias"
]
edge
[
  source 2
  target 7
  label "[s] Chefe/Beneficiário filtra a listagem por registros cujos beneficiários não possuem número do credor"
]
edge
[
  source 7
  target 8
  label "[e] system Identifica que a partir da listagem, registros cujos beneficiários não possuem número do credor; e  			Habilita a opção (por meio de item de tela) para que o usuário possa informar (individualmente) um número de credor para um beneficiário."
]
edge
[
  source 8
  target 9
  label "[s] Chefe/Beneficiário Informa o número do credor para o beneficiário; e Confirma a operação."
]
edge
[
  source 9
  target 5
  label "[e] system Realiza a persistência do número do credor, para o beneficiário indicado, na base do RH; Atualiza a listagem de solicitações aguardando empenho, já com o número do credor recém informado; e Exibe mensagem de sucesso."
]
edge
[
  source 2
  target 10
  label "[s] Chefe/Beneficiário Clica para atribuir/desatribuir o registro a si mesmo."
]
edge
[
  source 10
  target 5
  label "[e] system Atualiza a lista de registros de solicitações, onde o nome deverá constar o nome do usuário logado (que se atribuiu como responsável pelo empenho), no campo de atribuição (no caso de desatribuição, o nome deverá ser removido)."
]
edge
[
  source 2
  target 11
  label "[s] Chefe/Beneficiário Clica para realizar o empenho de uma diária."
]
edge
[
  source 11
  target 5
  label "[e] system Apresenta a tela de Registrar Empenho"
]
edge
[
  source 9
  target 5
  label "[e] system Identifica que houve um erro inesperado, quando da tentativa de inserção do número do credor; e Exibe mensagem de erro (MSG207 - Não foi possível atualizar o número do credor) para o usuário."
]
edge
[
  source 9
  target 5
  label "[e] system Identifica que o número de credor informado pelo usuário já existe (duplicidade); e Exibe mensagem de erro (MSG208 - Número do credor já existe) para o usuário."
]
]
