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
  label "[s] Chefe Clica para realizar a prestação de contas."
]
edge
[
  source 3
  target 4
  label "[e] system Exibe a lista de solicitações de diárias (histórico) em ordem decrescente DO NÚMERO DA DIÁRIA (da maior para a de menor número)."
]
edge
[
  source 4
  target 5
  label "[s] Chefe Indica uma solicitação de diárias a fim de realizar a prestação de contas."
]
edge
[
  source 5
  target 6
  label "[e] system Exibe os detalhes referentes à solicitação selecionada, bem como identificando e apresentando os tipos de documentos/comprovantes a serem informados/consultados pelo usuário; e Exibe o histórico da tramitação da prestação de contas."
]
edge
[
  source 6
  target 7
  label "[s] Chefe Visualiza o histório da tramitação da prestação de contas."
]
edge
[
  source 7
  target 8
  label "[e] system Exibe o histórico da tramitação da prestação de contas."
]
edge
[
  source 8
  target 9
  label "[c] O caso de uso encerra."
]
edge
[
  source 3
  target 9
  label "[e] system Identifica que se trata de um usuário com perfil OPERADOR; Exibe tanto as solicitações do próprio usuário quanto as solicitações dos demais servidores lotados na mesma unidade administrativa dele (OPERADOR)."
]
edge
[
  source 5
  target 9
  label "[e] system Identifica que a prestação de contas indicada pelo usuário não está em nenhum desses dois estados: a) NÃO REALIZADA e b) DEVOLVIDA; Permite não permite um novo envio ou alterações na prestação (exclusão de documentos)."
]
edge
[
  source 5
  target 9
  label "[e] system Identifica que a solicitação indicada pelo usuário ainda não pode ter sua prestação de contas realizada; Exibe mensagem de erro (MSG212 - Prestação de contas ainda não pode ser realizada) para o usuário, impedindo que ele preste contas (anexa arquivos e etc)."
]
edge
[
  source 6
  target 10
  label "[s] Chefe Clica em inserir; Seleciona um tipo de comprovante; Seleciona um arquivo."
]
edge
[
  source 10
  target 9
  label "[e] system Realiza o upload do arquivo indicado pelo usuário."
]
edge
[
  source 6
  target 11
  label "[s] Chefe Clica em visualizar comprovante."
]
edge
[
  source 11
  target 9
  label "[e] system Exibe modal com o comprovante."
]
edge
[
  source 6
  target 12
  label "[s] Chefe Clica em excluir comprovante."
]
edge
[
  source 12
  target 9
  label "[e] system Exclui o comprovante."
]
edge
[
  source 6
  target 13
  label "[s] Chefe Clica em confirmar (enviar prestação de contas)."
]
edge
[
  source 13
  target 14
  label "[e] system Exibe a mensagem de confirmação (MSG104 - Confirmar a prestação de contas) para o usuário."
]
edge
[
  source 14
  target 15
  label "[s] Chefe Confirma as mensagens, concordando com o prosseguimento da prestação de contas."
]
edge
[
  source 15
  target 9
  label "[e] system Altera o status da diária para PRESTAÇÃO DE CONTAS REALIZADA e salva os dados da solicitação; Realiza o registro do envio da prestação de contas para possibilitar posterior acompanhamento histórico; Exibe mensagem de sucesso (MSG302 - Operação realizada com sucesso!) para o usuário."
]
edge
[
  source 6
  target 16
  label "[s] Chefe Clica para detalhar a solicitação de diária."
]
edge
[
  source 16
  target 9
  label "[e] system Apresenta a tela de Detalhar Diárias"
]
edge
[
  source 13
  target 9
  label "[e] system Identifica que pelo menos um dos documentos solicitados para prestação de contas não foi anexado pelo usuário; Exibe mensagem de alerta (MSG001 - Documentos não informados na prestação de contas) para o usuário."
]
]
