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
  label "[s] Chefe Acessa a funcionalidade Minha Conta Bancária (menu)."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe a tela principal do sistema para o usuário; Recupera a partir do SRH as informações da conta bancária que o usuário utiliza para receber diárias; Exibe as informações da referida conta bancária para o usuário."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Visualiza os dados da referida conta bancária."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe mensagens informativas (MSG403 - Informativos sobre a atualização de conta bancária (dados bancários)) para o usuário sobre a manutenção de informações bancárias."
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
  label "[s] Chefe Altera os dados de sua conta bancária em tela (banco/agência/conta corrente)."
]
edge
[
  source 8
  target 9
  label "[e] system Apresenta os campos (banco/agência/conta corrente) alterados."
]
edge
[
  source 9
  target 10
  label "[s] Chefe Salva as alterações."
]
edge
[
  source 10
  target 11
  label "[e] system Exibe mensagem de confirmação (MSG106 - Confirma a alteração dos dados bancários para recebimento de diárias?)."
]
edge
[
  source 11
  target 12
  label "[s] Chefe Confirma."
]
edge
[
  source 12
  target 7
  label "[e] system Atualiza os dados bancários do beneficiário na base do RH (SRH); Exibe mensagem de sucesso para o usuário."
]
edge
[
  source 12
  target 7
  label "[e] system Identifica que ocorreu uma falha durante a tentativa de atualização dos dados bancários; Mantém os dados consistentes, interrompe a operação; Exibe mensagem de erro (MSG213 - Não foi possível concluir a operação. Falha na comunicação com o sistema de Recursos Humanos) para o usuário."
]
edge
[
  source 12
  target 7
  label "[e] system Identifica que há dados obrigatórios não informados; Exibe mensagem de erro (MSG203 - Campos obrigatórios) para o usuário."
]
]
