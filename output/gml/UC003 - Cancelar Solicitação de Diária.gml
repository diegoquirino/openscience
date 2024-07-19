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
  label "[c] O usuario devidamente autenticado e na tela inicial de cancelar diárias"
]
edge
[
  source 2
  target 3
  label "[s] Chefe Informa o motivo do cancelamento."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe a mensagem (MSG102 - Confirmar cancelamento)"
]
edge
[
  source 4
  target 5
  label "[s] Chefe Clica em confirmar."
]
edge
[
  source 5
  target 6
  label "[e] system Altera o status da diária para CANCELADA."
]
edge
[
  source 6
  target 7
  label "[c] O caso de uso encerra."
]
edge
[
  source 2
  target 8
  label "[s] Chefe Não informa o motivo do cancelamento."
]
edge
[
  source 8
  target 9
  label "[e] system Exibe a mensagem (MSG102 - Confirmar cancelamento)"
]
edge
[
  source 9
  target 5
  label "[s] Chefe Clica em confirmar."
]
edge
[
  source 3
  target 7
  label "[e] system Identifica que a solicitação de diária está em situação diferente de 'SOLICITADA PARA EMPENHO' ou 'SOLICITADA PARA PRESTAÇÃO DE CONTAS'.  Impede o cancelamento e exibe mensagem de erro (MSG205 - Solcitação de diária não pode ser cancelada) para o usuário."
]
edge
[
  source 5
  target 7
  label "[e] system Identifica que o usuário não informou uma justificativa para o cancelamento. Não efetiva o cancelamento e exibe mensagem de erro (MSG217 - Necessário informar uma justificativa para o cancelamento de solicitações	) para o usuário."
]
]
