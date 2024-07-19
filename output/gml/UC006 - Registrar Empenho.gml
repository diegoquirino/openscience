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
  label "[c] O sistema identifica a solicitação a ser empenhada (recebida por parâmetro). O sistema exibe os dados da solicitação para o usuário."
]
edge
[
  source 2
  target 3
  label "[s] Chefe Informa o número do empenho e do exercício."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe o número de empenho no campo."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Confirma a operação."
]
edge
[
  source 5
  target 6
  label "[e] system Persiste o número do empenho e o ano de exercício conforme informados; Altera a situação da diária (solicitação) para EMPENHADA; e Exibe mensagem de sucesso para o usuário."
]
edge
[
  source 6
  target 7
  label "[c] O caso de uso encerra."
]
edge
[
  source 5
  target 7
  label "[e] system Identifica que campos obrigatórios não foram preenchidos; e Exibe mensagem de erro (MSG203 - Campos obrigatórios / MSG216 - É obrigatório informar o número do empenho) para o usuário."
]
edge
[
  source 5
  target 7
  label "[e] system Verifica que já existe um número de empenho (considerando também o mesmo exercício) registrado na base de dados; e Exibe mensagem de erro (MSG209 - Número do empenho já existe) para o usuário."
]
]
