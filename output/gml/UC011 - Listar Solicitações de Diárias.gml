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
  label "[s] Chefe Clica para listar todas as solicitações de diárias relacionadas à sua competência."
]
edge
[
  source 3
  target 4
  label "[e] system Recupera os registros de solicitações e os exibe (em ordem decrescente pelo número da diária) em tela para o usuário."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Visualiza os registros de solicitações de diária."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe os registros de solicitações de diária."
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
  label "[e] system Exibe o detalhamento em modal para o usuário; Apresenta a tela de Detalhar Diárias"
]
edge
[
  source 4
  target 9
  label "[s] Chefe Clica para realizar o cancelamento de uma diária."
]
edge
[
  source 9
  target 7
  label "[e] system Verifica que a solicitação está em situação SOLICITADA; Exibe mensagem de confirmação (MSG987 - Cancelar solicitação de diária) para o usuário (que deve confirmar); Cancela a diária, mudando sua situação para CANCELADA (ver diagrama de estados da diária)."
]
edge
[
  source 4
  target 10
  label "[s] Chefe Indica alguns parâmetros específicos para a busca; Informa o nome do beneficiário; Filtra a listagem de solicitações."
]
edge
[
  source 10
  target 7
  label "[e] system Exibe uma nova listagem de solicitações, de acordo com os filtros informados pelo usuário."
]
edge
[
  source 4
  target 11
  label "[s] Chefe Clica para ordenar pelo nome do servidor."
]
edge
[
  source 11
  target 7
  label "[e] system Visualiza os registros de solicitações de diária ordenado pelo nome do servidor."
]
]
