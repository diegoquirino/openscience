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
  label "[s] Beneficiário O usuario acessa o caso de uso atraves do menu."
]
edge
[
  source 3
  target 4
  label "[e] system Apresenta a tela com a lista de diárias daquele beneficiário."
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
  label "[s] Beneficiário Informa os filtros desejados."
]
edge
[
  source 6
  target 5
  label "[e] system Atualiza a lista de diárias conforme os filtros."
]
edge
[
  source 2
  target 7
  label "[s] Beneficiário Clica em detalhar diária."
]
edge
[
  source 7
  target 5
  label "[e] system Apresenta a tela de Detalhar Diárias"
]
edge
[
  source 2
  target 8
  label "[s] Beneficiário Clica em analisar prestação de contas."
]
edge
[
  source 8
  target 5
  label "[e] system Apresenta a tela de Analisar Prestação de Contas"
]
edge
[
  source 2
  target 9
  label "[s] Beneficiário Clica em cancelar diária."
]
edge
[
  source 9
  target 5
  label "[e] system Apresenta a tela de Cancelar Solicitação de Diária"
]
]
