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
edge
[
  source 1
  target 2
  label "[c] O usuário devidamente autenticado e na tela inicial do sistema."
]
edge
[
  source 2
  target 3
  label "[s] Chefe Clica para exibir a lista de diárias (solicitações) aptas para pagamento (SITUAÇÃO EMPENHADA)."
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
  label "[s] Chefe Visualiza a lista de diárias (solicitações) aptas para pagamento."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe a lista de diárias (solicitações) aptas para pagamento ordenado pelo número da diária em ordem crescente. Exibe esta lista de diárias também ordenada pela data de chegada da solicitação na fase de liquidação (após registrar o empenho)."
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
  label "[e] system Apresenta a tela de Detalhar Diárias."
]
edge
[
  source 4
  target 9
  label "[s] Chefe Clica para atribuir/desatribuir o registro a si mesmo."
]
edge
[
  source 9
  target 7
  label "[e] system Atualiza a lista de registros de solicitações, onde deverá constar o nome do usuário logado (que se atribuiu como responsável pela liquidação) no campo de atribuição (no caso de desatribuição, o nome deverá ser removido)."
]
edge
[
  source 4
  target 10
  label "[s] Chefe Clica para realizar a liquidação."
]
edge
[
  source 10
  target 7
  label "[e] system Apresenta a tela de Registrar Liquidações."
]
]
