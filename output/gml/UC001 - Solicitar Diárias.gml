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
node
[
  id 16
  label "16"
]
node
[
  id 17
  label "17"
]
node
[
  id 18
  label "18"
]
node
[
  id 19
  label "19"
]
node
[
  id 20
  label "20"
]
node
[
  id 21
  label "21"
]
node
[
  id 22
  label "22"
]
node
[
  id 23
  label "23"
]
node
[
  id 24
  label "24"
]
node
[
  id 25
  label "25"
]
node
[
  id 26
  label "26"
]
node
[
  id 27
  label "27"
]
node
[
  id 28
  label "28"
]
node
[
  id 29
  label "29"
]
node
[
  id 30
  label "30"
]
node
[
  id 31
  label "31"
]
node
[
  id 32
  label "32"
]
node
[
  id 33
  label "33"
]
node
[
  id 34
  label "34"
]
node
[
  id 35
  label "35"
]
node
[
  id 36
  label "36"
]
node
[
  id 37
  label "37"
]
node
[
  id 38
  label "38"
]
node
[
  id 39
  label "39"
]
node
[
  id 40
  label "40"
]
node
[
  id 41
  label "41"
]
node
[
  id 42
  label "42"
]
node
[
  id 43
  label "43"
]
node
[
  id 44
  label "44"
]
edge
[
  source 1
  target 2
  label "[c] O usuário acessa o caso de uso através do menu.. O sistema exibe a tela de solicitação de diárias."
]
edge
[
  source 2
  target 3
  label "[s] Chefe/Beneficiário Escolhe o tipo de viagem nacional - fora do estado (interestadual)."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe a opção escolhida: viagem nacional - fora do estado (interestadual)."
]
edge
[
  source 4
  target 5
  label "[s] Chefe/Beneficiário Escolhe o estado."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe a opção escolhida: estado."
]
edge
[
  source 6
  target 7
  label "[s] Chefe/Beneficiário Escolhe a(s) cidade(s)."
]
edge
[
  source 7
  target 8
  label "[e] system Exibe a opção escolhida: cidade(s)."
]
edge
[
  source 8
  target 9
  label "[s] Chefe/Beneficiário Escolhe o tipo de deslocamento."
]
edge
[
  source 9
  target 10
  label "[e] system Exibe a opção escolhida: tipo de deslocamento."
]
edge
[
  source 10
  target 11
  label "[s] Chefe/Beneficiário Escolhe o tipo de hospedagem."
]
edge
[
  source 11
  target 12
  label "[e] system Exibe a opção escolhida: hospedagem."
]
edge
[
  source 12
  target 13
  label "[s] Chefe/Beneficiário Escolhe o tipo de período de afastamento."
]
edge
[
  source 13
  target 14
  label "[e] system Exibe a opção escolhida: período de afastamento."
]
edge
[
  source 14
  target 15
  label "[s] Chefe/Beneficiário Informa as datas de afastamento, antes do último dia de viagem."
]
edge
[
  source 15
  target 16
  label "[e] system Exibe as datas escolhidas: datas de afastamento."
]
edge
[
  source 16
  target 17
  label "[s] Chefe/Beneficiário Informa se tem pernoite."
]
edge
[
  source 17
  target 18
  label "[e] system Exibe a opção escolhida: pernoite."
]
edge
[
  source 18
  target 19
  label "[s] Chefe/Beneficiário Seleciona a justificativa."
]
edge
[
  source 19
  target 20
  label "[e] system Exibe a opção escolhida: justificativa."
]
edge
[
  source 20
  target 21
  label "[s] Chefe/Beneficiário Detalha a justificativa."
]
edge
[
  source 21
  target 22
  label "[e] system Exibe o texto informado: detalhe da justificativa."
]
edge
[
  source 22
  target 23
  label "[s] Chefe/Beneficiário Seleciona o(s) beneficiário(s) da(s) diária(s)."
]
edge
[
  source 23
  target 24
  label "[e] system Exibe os beneficiário(s) selecionado(s)."
]
edge
[
  source 24
  target 25
  label "[s] Chefe/Beneficiário Seleciona o(s) beneficiário(s) da(s) diária(s)."
]
edge
[
  source 25
  target 26
  label "[e] system Confirma a existência de conta para recebimento de diárias do servidor."
]
edge
[
  source 26
  target 27
  label "[s] Chefe/Beneficiário Clica em confirmar."
]
edge
[
  source 27
  target 28
  label "[e] system Calcula o valor da(s) diária(s) com as informações do formulário."
]
edge
[
  source 28
  target 29
  label "[s] Chefe/Beneficiário Verifica os valores presentes na tela: 1. Magistrados e desembargadores: não podem ultrapassar 60% do valor de uma diária de um ministro do STF. 2. Servidores não podem ultrapassar 60% do valor de uma diária de um Magistrado. 3. Manutenção do cargo comissionado e do setor beneficiário à época da concessão da diária."
]
edge
[
  source 29
  target 30
  label "[e] system Altera o status da diária para SOLICITADA: (antes) para empenho; ou, (depois) para prestação de contas."
]
edge
[
  source 30
  target 31
  label "[c] O sistema salva os dados."
]
edge
[
  source 2
  target 32
  label "[s] Chefe/Beneficiário Seleciona o tipo de viagem nacional - dentro do estado (intermunicipal)."
]
edge
[
  source 32
  target 33
  label "[e] system Exibe a opção escolhida: viagem nacional - dentro do estado (intermunicipal)."
]
edge
[
  source 33
  target 34
  label "[s] Chefe/Beneficiário NÃO Escolhe o estado."
]
edge
[
  source 34
  target 35
  label "[e] system O sistema seleciona o estado como PB, automaticamente."
]
edge
[
  source 35
  target 7
  label "[s] Chefe/Beneficiário Escolhe a(s) cidade(s)."
]
edge
[
  source 2
  target 36
  label "[s] Chefe/Beneficiário Seleciona o tipo de viagem internacional."
]
edge
[
  source 36
  target 37
  label "[e] system Exibe a lista de países para seleção, no lugar de estado/cidade."
]
edge
[
  source 37
  target 38
  label "[s] Chefe/Beneficiário Seleciona o país da viagem."
]
edge
[
  source 38
  target 39
  label "[e] system Exibe a opção escolhida: país da viagem."
]
edge
[
  source 39
  target 9
  label "[s] Chefe/Beneficiário Escolhe o tipo de deslocamento."
]
edge
[
  source 5
  target 31
  label "[e] system Exibe a mensagem de erro MSG201 - DNE indisponível."
]
edge
[
  source 14
  target 15
  label "[s] Chefe/Beneficiário Informa as datas de afastamento, após o último dia de viagem."
]
edge
[
  source 22
  target 40
  label "[s] Chefe/Beneficiário Seleciona o(s) beneficiário(s) da(s) diária(s)."
]
edge
[
  source 40
  target 41
  label "[e] system Exibe os beneficiário(s) selecionado(s) e o campo para preenchimento do nome social."
]
edge
[
  source 41
  target 25
  label "[s] Chefe/Beneficiário Informa o nome social do beneficiário da(s) diária(s)."
]
edge
[
  source 23
  target 31
  label "[e] system Não confirma a existência de conta para recebimento de diárias do servidor. Exibe a mensagem de erro MSG002 - Conta para recebimento de diárias não cadastrada."
]
edge
[
  source 25
  target 31
  label "[e] system Exibe a mensagem de erro MSG202 - RGP Core indisponível."
]
edge
[
  source 26
  target 42
  label "[s] Chefe/Beneficiário Clica em limpar campos."
]
edge
[
  source 42
  target 31
  label "[e] system Apaga todas as seleções do usuário."
]
edge
[
  source 26
  target 43
  label "[s] Chefe/Beneficiário Clica em confirmar."
]
edge
[
  source 43
  target 44
  label "[e] system Exibe uma mensagem de alerta informando sobre o estouro do limite de 10 diárias por mês."
]
edge
[
  source 44
  target 27
  label "[s] Chefe/Beneficiário Fecha o alerta"
]
edge
[
  source 27
  target 31
  label "[e] system Exibe a mensagem de erro MSG203 - Campos obrigatórios, MSG214 - Campos obrigatórios da solicitação de diárias (não informados)."
]
edge
[
  source 27
  target 31
  label "[e] system Exibe a mensagem de erro MSG204 - Conflito de duplicidade de diárias."
]
]
