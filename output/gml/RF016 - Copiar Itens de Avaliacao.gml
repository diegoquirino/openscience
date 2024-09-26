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
edge
[
  source 1
  target 2
  label "[c] Lider de Pessoas esta autenticado no sistema e tem permissao para copiar itens de avaliacoes"
]
edge
[
  source 2
  target 3
  label "[s] Lider de Pessoas acessa a funcionalidade 'Avaliacoes' a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem das Avaliacoes cadastradas com a opcao 'Copiar' dentre as varias exibidas"
]
edge
[
  source 4
  target 5
  label "[s] Lider de Pessoas com uma Avaliacao selecionada, clica na opcao 'Copiar' para copia-la"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para copiar a Avaliacao com os valores atuais ja preenchidos, exceto o 'Avaliado', que deve estar vazio"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas seleciona o 'Periodo Avaliativo' da Avaliacao"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Periodo Avaliativo' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Lider de Pessoas seleciona o colaborador a ser 'Avaliado' na Avaliacao"
]
edge
[
  source 9
  target 10
  label "[e] system apresenta o campo 'Avaliado' preenchido corretamente"
]
edge
[
  source 10
  target 11
  label "[s] Lider de Pessoas escolhe que deseja 'fazer uma copia' do Perfil de Competencias"
]
edge
[
  source 11
  target 12
  label "[e] system apresenta o campo 'fazer uma copia' do Perfil de Competencias com a opcao 'SIM' selecionada"
]
edge
[
  source 12
  target 13
  label "[s] Lider de Pessoas preenche o 'nome' do novo Perfil de Competencias da Avaliacao"
]
edge
[
  source 13
  target 14
  label "[e] system apresenta o campo 'nome' do novo Perfil de Competencias da Avaliacao preenchido corretamente"
]
edge
[
  source 14
  target 15
  label "[s] Lider de Pessoas clica na opcao 'Confirmar'"
]
edge
[
  source 15
  target 16
  label "[e] system apresenta uma mensagem de confirmacao referente a criacao do novo Perfil de Competencias"
]
edge
[
  source 16
  target 17
  label "[s] Lider de Pessoas confirma a operacao de criacao do novo Perfil de Competencias"
]
edge
[
  source 17
  target 18
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e      retorna feedback correspondente"
]
edge
[
  source 18
  target 19
  label "[c] A copia de itens de avaliacao e realizada com sucesso."
]
edge
[
  source 2
  target 20
  label "[s] Lider de Pessoas acessa a funcionalidade 'Catalogo (Perfis) de Competencias' a partir do menu inicial"
]
edge
[
  source 20
  target 21
  label "[e] system exibe a listagem dos Perfis de Competencias com a opcao 'Copiar' dentre as varias exibidas"
]
edge
[
  source 21
  target 22
  label "[s] Lider de Pessoas com um Perfil de Competencias selecionado, clica na opcao 'Copiar' para copia-lo"
]
edge
[
  source 22
  target 23
  label "[e] system apresenta o formulario para copiar o Perfil de Competencias com os valores atuais ja preenchidos"
]
edge
[
  source 23
  target 24
  label "[s] Lider de Pessoas seleciona o 'Periodo Avaliativo' da Avaliacao"
]
edge
[
  source 24
  target 25
  label "[e] system apresenta o campo 'Periodo Avaliativo' preenchido corretamente"
]
edge
[
  source 25
  target 26
  label "[s] Lider de Pessoas preenche o 'nome' do novo Perfil de Competencias da Avaliacao"
]
edge
[
  source 26
  target 27
  label "[e] system apresenta o campo 'nome' do novo Perfil de Competencias preenchido corretamente"
]
edge
[
  source 27
  target 17
  label "[s] Lider de Pessoas clica na opcao 'Confirmar'"
]
edge
[
  source 10
  target 28
  label "[s] Lider de Pessoas escolhe que nao deseja 'fazer uma copia' do Perfil de Competencias"
]
edge
[
  source 28
  target 29
  label "[e] system apresenta o campo 'fazer uma copia' do Perfil de Competencias com a opcao 'NAO' selecionada"
]
edge
[
  source 29
  target 30
  label "[s] Lider de Pessoas seleciona um Perfil de Competencias dentre os disponiveis"
]
edge
[
  source 30
  target 31
  label "[e] system apresenta o 'nome' do Perfil de Competencias existente escolhido"
]
edge
[
  source 31
  target 17
  label "[s] Lider de Pessoas clica na opcao 'Confirmar'"
]
edge
[
  source 14
  target 32
  label "[s] Lider de Pessoas nao confirma a operacao de criacao do novo Perfil de Competencias"
]
edge
[
  source 32
  target 19
  label "[e] system apresenta o formulario para copiar a Avaliacao com os valores ja preenchidos"
]
edge
[
  source 16
  target 33
  label "[s] Lider de Pessoas nao confirma a operacao de criacao do novo Perfil de Competencias"
]
edge
[
  source 33
  target 19
  label "[e] system exibe a listagem de Avaliacoes sem uma nova avaliacao copiada"
]
edge
[
  source 17
  target 19
  label "[e] system exibe uma mensagem de erro ao tentar salvar a nova Avaliacao, informando o campo ou a validacao que falhou"
]
edge
[
  source 17
  target 19
  label "[e] system exibe uma mensagem de erro ao tentar copiar a Avaliacao, informando que o Perfil de Competencias com este nome ja existe"
]
]
