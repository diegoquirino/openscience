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
edge
[
  source 1
  target 2
  label "[c] O usuário devidamente autenticado e na tela de listagem de diárias."
]
edge
[
  source 2
  target 3
  label "[s] Chefe/Beneficiário Solicita o detalhamento de uma diária."
]
edge
[
  source 3
  target 4
  label "[e] system Apresenta a tela com as informações da diária do servidor (dados gerais da diária, estratificação do cálculo da diária, conta para crédito, período da viagem, histórico de tramitação, detalhamento de empenho e pagamento, prestação de contas e seus detalhes). Apresenta o número do empenho e a autorização de pagamento como links. Apresenta, no detalhamento das diárias, a justificativa concatenada com o detalhe."
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
  label "[s] Chefe/Beneficiário Clica em detalhar empenho."
]
edge
[
  source 6
  target 5
  label "[e] system Monta a URL do pagamento da diária; e Direciona o usuário para a aba de empenho através da URL."
]
edge
[
  source 2
  target 7
  label "[s] Chefe/Beneficiário Clica em detalhar pagamento."
]
edge
[
  source 7
  target 5
  label "[e] system Monta a URL do pagamento da diária; e Acessa a URL do pagamento."
]
]
