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
  label "[s] Chefe Acessa a funcionalidade de prestação de contas."
]
edge
[
  source 3
  target 4
  label "[e] system Apresenta a lista de prestações de contas relacionadas à sua competência (em ordem decrescente DO NÚMERO DA DIÁRIA, da maior para a de menor número)."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Indica uma prestação de contas para analisar."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe os detalhes relativos àquela prestação de contas (dados básicos da solicitação e documentos anexos); Exibe o histórico da tramitação da prestação de contas."
]
edge
[
  source 6
  target 7
  label "[s] Chefe Clica para analisar a prestação de contas."
]
edge
[
  source 7
  target 8
  label "[e] system Exibe a tela para prestação de contas"
]
edge
[
  source 8
  target 9
  label "[s] Chefe Informa o parecer da prestação de contas."
]
edge
[
  source 9
  target 10
  label "[e] system Exibe mensagem de confirmação (MSG105 - Confirmar parecer da prestação de contas) para o usuário."
]
edge
[
  source 10
  target 11
  label "[s] Chefe Confirma o parecer dado (confirma operação)."
]
edge
[
  source 11
  target 12
  label "[e] system Persiste as informações, alterando o status da prestação de contas conforme informada pelo usuário; Realiza o registro do envio da prestação de contas para possibilitar posterior acompanhamento histórico; Exibe mensagem de sucesso (MSG302 - Operação realizada com sucesso!) para o usuário."
]
edge
[
  source 12
  target 13
  label "[c] O caso de uso encerra."
]
edge
[
  source 5
  target 13
  label "[e] system Identifica que a prestação de contas escolhida é diferente de REALIZADA; Não permite que seja feita análise por parte do usuário."
]
edge
[
  source 10
  target 14
  label "[s] Chefe Clica no documento a exibir."
]
edge
[
  source 14
  target 13
  label "[e] system Exibe o documento em tela para o usuário."
]
edge
[
  source 10
  target 15
  label "[s] Chefe Clica para detalhar a solicitação relacionada a uma determinada prestação de contas."
]
edge
[
  source 15
  target 13
  label "[e] system Exibe a tela de Detalhar Diárias"
]
edge
[
  source 11
  target 13
  label "[e] system Identifica que campos obrigatórios do parecer/análise não foram devidamente preenchidos Exibe mensagem de erro (MSG203 - Campos obrigatórios) para o usuário."
]
]
