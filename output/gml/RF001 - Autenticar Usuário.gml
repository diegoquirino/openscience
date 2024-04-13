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
  label "[c] Usuário se encontra na tela inicial do sistema Usuário está visualizando uma mensagem de erro informando que o acesso à página não é permitido para usuários sem autenticação"
]
edge
[
  source 2
  target 3
  label "[s] Usuário do Sistema inicia a tela de login através da opção de login no canto superior direito"
]
edge
[
  source 3
  target 4
  label "[e] system apresenta um formulário com campos de nome de usuário e senha e um botão entrar"
]
edge
[
  source 4
  target 5
  label "[s] Usuário do Sistema preenche os campos e clica no botão entrar"
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
  label "[c] Usuário logado com sucesso"
]
edge
[
  source 4
  target 5
  label "[s] Usuário do Sistema seleciona um nome de usuário sugerido, digita a senha e clica no botão entrar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o TJSeg (sistema que fornece as permissões de acesso e escrita) está fora do ar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o CAS (sistema de autorização login-senha) está fora do ar"
]
edge
[
  source 5
  target 4
  label "[e] system alerta que o nome de usuário e/ou senha estão incorretos"
]
]
