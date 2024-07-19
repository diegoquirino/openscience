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
  label "[c] O sistema identifica a solicitação a ter a liquidação registrada (recebida por parâmetro). Exibe os dados da solicitação para o usuário."
]
edge
[
  source 2
  target 3
  label "[s] Chefe Informa o número da liquidação e do exercício."
]
edge
[
  source 3
  target 4
  label "[e] system Apresenta os campos número de liquidação e exercício preenchidos."
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
  label "[e] system Persiste o número da liquidação e o ano de exercício conforme informados; Altera a situação da diária (solicitação) para LIQUIDADA; Exibe mensagem de sucesso (MSG302 - Operação realizada com sucesso!) para o usuário."
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
  label "[e] system Identifica que campos obrigatórios não foram preenchidos; Exibe mensagem adequada de erro (MSG203 - Campos obrigatórios / MSG218 - É obrigatório informar o número da liquidação) para o usuário."
]
edge
[
  source 5
  target 7
  label "[e] system Verifica que já existe um número de liquidação (considerando também o mesmo exercício) registrado na base de dados; Interrompe a operação e exibe mensagem de erro (MSG211 - Número da liquidação já existe) para o usuário."
]
]
