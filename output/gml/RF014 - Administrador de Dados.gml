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
  label "[c] Administrador esta autenticado no sistema e tem permissao para alterar Gerente de Desempenho"
]
edge
[
  source 2
  target 3
  label "[s] Administrador acessa a funcionalidade de 'Catalogo (Perfis) de Competencias' a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem dos Perfis de Competencias cadastrados com a opcao 'Alterar Gerente' dentre as varias exibidas"
]
edge
[
  source 4
  target 5
  label "[s] Administrador com um perfil selecionado, clica na opcao 'Alterar Gerente' para efetivar a mudanca"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario para cadastro do novo Gerente de Desempenho"
]
edge
[
  source 6
  target 7
  label "[s] Administrador preenche o campo 'Login do Novo Gerente de Desempenho' para o Perfil de Competencias"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Login do Novo Gerente de Desempenho' preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Administrador clica na opcao 'Confirmar'"
]
edge
[
  source 9
  target 10
  label "[e] system realiza a acao conforme a opcao selecionada pelo usuario e     retorna feedback correspondente"
]
edge
[
  source 10
  target 11
  label "[c] A alteracao do Gerente de Desempenho e realizada com sucesso."
]
edge
[
  source 8
  target 12
  label "[s] Administrador clica na opcao 'Cancelar'"
]
edge
[
  source 12
  target 11
  label "[e] system apresenta o Catalogo (Perfis) de Competencias sem nenhuma alteracao"
]
edge
[
  source 9
  target 11
  label "[e] system exibe uma mensagem de erro ao tentar salvar, informando o campo ou a validacao que falhou"
]
]
