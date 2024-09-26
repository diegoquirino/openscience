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
edge
[
  source 1
  target 2
  label "[c] Lider de Pessoas esta autenticado no sistema e tem permissao para gerenciar Metas de Desempenho"
]
edge
[
  source 2
  target 3
  label "[s] Lider de Pessoas acessa a funcionalidade de Avaliacoes a partir do menu inicial"
]
edge
[
  source 3
  target 4
  label "[e] system exibe a listagem das Avaliacoes cadastradas com a opcao 'Editar' dentre as varias exibidas"
]
edge
[
  source 4
  target 5
  label "[s] Lider de Pessoas com uma avaliacao selecionada, clica na opcao 'Editar' para modificar a Avaliacao de Desempenho"
]
edge
[
  source 5
  target 6
  label "[e] system apresenta o formulario com o campo 'Metas' contendo cada Competencia do perfil avaliado"
]
edge
[
  source 6
  target 7
  label "[s] Lider de Pessoas altera o valor da 'Meta' para cada Competencia listada do perfil avaliado na Avaliacao atual (valores padrao de 1 a 4)"
]
edge
[
  source 7
  target 8
  label "[e] system apresenta o campo 'Meta' para cada Competencia listada do perfil avaliado na Avaliacao preenchido corretamente"
]
edge
[
  source 8
  target 9
  label "[s] Lider de Pessoas clica na opcao 'Salvar'"
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
  label "[c] A gestao de Metas de Desempenho e realizada com sucesso."
]
edge
[
  source 9
  target 11
  label "[e] system exibe uma mensagem de erro ao tentar editar, informando o campo ou a validacao que falhou"
]
]
