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
  label "[c] Usuario nao-autenticado no sistema se encontra na tela inicial do sistema"
]
edge
[
  source 2
  target 3
  label "[s] Usuario Nao-Autenticado acessa a funcionalidade para visualizar o status do PGDC"
]
edge
[
  source 3
  target 4
  label "[e] system apresenta o campo 'Periodo Avaliativo' para ser escolhido pelo usuario"
]
edge
[
  source 4
  target 5
  label "[s] Usuario Nao-Autenticado escolhe no campo 'Periodo Avaliativo' o ano correspondente"
]
edge
[
  source 5
  target 6
  label "[e] system exibe na pagina uma tabela contendo as informacoes (basicamente listagem dos perfis de competencias - cf. RF006):                    Perfil de Competencias; Unidade de Trabalho; Gerente do Desempenho; e, Situacao."
]
edge
[
  source 6
  target 7
  label "[c] Usuario visualiza a tabela das Permissoes concedidas ao seu usuario"
]
edge
[
  source 3
  target 7
  label "[e] system exibe uma mensagem de erro informando que nao existem Periodos Avaliativos cadastrados"
]
]
