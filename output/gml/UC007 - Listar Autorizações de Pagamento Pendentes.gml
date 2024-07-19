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
  label "[c] O usuario devidamente autenticado e na tela inicial do sistema"
]
edge
[
  source 2
  target 3
  label "[s] Chefe Clica para exibir a lista de diárias (solicitações) aptas para pagamento (SITUAÇÃO LIQUIDADA)."
]
edge
[
  source 3
  target 4
  label "[e] system Recupera e exibe para o usuário a lista de diárias aptas para pagamento."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Seleciona uma diária apta para pagamento"
]
edge
[
  source 5
  target 6
  label "[e] system Destaca a diária selecionada"
]
edge
[
  source 6
  target 7
  label "[c] O caso de uso encerra."
]
edge
[
  source 4
  target 8
  label "[s] Chefe Clica para detalhar a solicitação de diária."
]
edge
[
  source 8
  target 7
  label "[e] system Apresenta a tela de Detalhar Diárias"
]
edge
[
  source 4
  target 9
  label "[s] Chefe Dado um registro selecionado (solicitação aguardando autorização de pagamento - AP), o usuário pode atribuir/desatribuir a responsabilidade da AP a si próprio; e Clica para atribuir/desatribuir o registro a si mesmo."
]
edge
[
  source 9
  target 7
  label "[e] system Atualiza a lista de registros de solicitações, onde o nome deverá constar o nome do usuário logado (que se atribuiu como responsável pela AP) no campo de atribuição (no caso de desatribuição, o nome deverá ser removido)."
]
edge
[
  source 4
  target 10
  label "[s] Chefe Clica para realizar a autorização de pagamento."
]
edge
[
  source 10
  target 7
  label "[e] system Apresenta a tela de Registrar Autorizações de Pagamento"
]
edge
[
  source 4
  target 11
  label "[s] Chefe Seleciona um usuário para filtrar as autorizações de pagamento associadas a ele; e Submete a busca ao sistema."
]
edge
[
  source 11
  target 7
  label "[e] system Filtra os registros (autorizações de pagamento pendentes) e exibe apenas aqueles atribuídos ao usuário selecionado."
]
]
