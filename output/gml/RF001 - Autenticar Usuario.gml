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
  label "[c] Usuario se encontra na tela inicial do sistema Usuario esta visualizando uma mensagem de erro informando que o acesso a pagina nao e permitido para usuarios sem autenticacao"
]
edge
[
  source 2
  target 3
  label "[s] Usuario do Sistema inicia a tela de login atraves da opcao de Login no canto superior direito"
]
edge
[
  source 3
  target 4
  label "[e] system apresenta um formulario com campos de nome de usuario e senha e um botao entrar"
]
edge
[
  source 4
  target 5
  label "[s] Usuario do Sistema preenche os campos e clica no botao entrar"
]
edge
[
  source 5
  target 6
  label "[e] system exibe uma mensagem de sucesso"
]
edge
[
  source 6
  target 7
  label "[c] Usuario logado com sucesso"
]
edge
[
  source 4
  target 5
  label "[s] Usuario do Sistema seleciona um nome de usuario sugerido, digita a senha e clica no botao entrar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o TJSeg (sistema que fornece as permissoes de acesso e escrita) esta fora do ar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o CAS (sistema de autorizacao login-senha) esta fora do ar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o nome de usuario e/ou senha estao incorretos"
]
]
