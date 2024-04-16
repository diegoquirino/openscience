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
edge
[
  source 1
  target 2
  label "[c] Usuário autenticado no sistema (em qualquer tela)"
]
edge
[
  source 2
  target 3
  label "[s] Usuário do Sistema clica na opção de logout através da opção disponível no canto superior direito"
]
edge
[
  source 3
  target 4
  label "[e] system exibe uma mensagem de sucesso e apresenta a tela inicial do sistema"
]
edge
[
  source 4
  target 5
  label "[c] Usuário deslogado com sucesso"
]
]
