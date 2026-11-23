-- FIXTURE for local staging and automated tests. Draft TCLE/LGPD text for manual verification only.
-- Aligned with ISSTA_2027/CEP-TCLE/03_tcle_virtual (dev preview). NOT approved by CEP.
-- Never copy this file into db/migration: production must not load it (spec 006).
--
-- Acknowledgments: positive radios on the consent UI (spec 027); acknowledgment_anonymous when "Não".
-- WebPós PPGCC 2021.2 (researcher identity for CEP/TCLE drafts; not participant PII):
--   Carlos Diego Quirino Lima | matrícula 0123015808-15D | DOUTORADO | doutorando
--   e-mail WebPós: diegoquirino@gmail.com | admissão 07/03/2023 | conclusão prevista 07/03/2027
--   orientadores: Wilkerson de Lucena Andrade; Everton Leandro Galdino Alves
INSERT INTO consent_form (version, reference_language, created_at, published_at)
VALUES ('fixture-1', 'pt-BR', '2000-01-01 00:00:00.000000', '2000-01-01 00:00:00.000000');

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'pt-BR', 'TERMO DE CONSENTIMENTO LIVRE E ESCLARECIDO - FIXTURE / RASCUNHO DEV - NÃO APROVADO PELO CEP

Este texto é rascunho para staging/dev-local, alinhado ao pacote CEP-TCLE. Não é o termo definitivo e não foi aprovado pelo Comitê de Ética em Pesquisa (CEP).

TERMO DE CONSENTIMENTO LIVRE E ESCLARECIDO (TCLE)
Pesquisa em ambiente virtual

Título do projeto: Semantic Change Impact Analysis Framework for System-Level Test Case Maintenance  
(Framework de Análise de Impacto de Mudanças Semânticas para Manutenção de Casos de Teste em Nível de Sistema)

Você está sendo convidado(a) a participar do projeto de pesquisa acima citado, desenvolvido sob responsabilidade de Carlos Diego Quirino Lima, doutorando do Programa de Pós-Graduação em Ciência da Computação (PPGCC), Centro de Engenharia Elétrica e Informática (CEEI), Universidade Federal de Campina Grande (UFCG), matrícula 0123015808-15D, sob orientação do Prof. Dr. Wilkerson de Lucena Andrade e coorientação do Prof. Dr. Everton Leandro Galdino Alves.

O presente documento contém as informações necessárias sobre a pesquisa. Sua colaboração é importante, mas você pode recusar ou desistir a qualquer momento, sem prejuízo, penalidade ou necessidade de justificar.

Este estudo ocorre inteiramente em ambiente virtual, por meio da plataforma Veredito (aplicação web acessível pela internet), em conformidade com o Ofício Circular nº 02/2021/CONEP/SECNS/MS, a Resolução CNS nº 466/2012, a Resolução CNS nº 510/2016 (quando aplicável) e a Lei nº 13.709/2018 (LGPD).



Ao manifestar consentimento eletronicamente na plataforma Veredito, você declara que concorda de livre e espontânea vontade em participar como voluntário(a) do estudo Semantic Change Impact Analysis Framework for System-Level Test Case Maintenance, e que obteve as informações necessárias e os esclarecimentos quanto às dúvidas por você apresentadas.

Estou ciente de que:

I) Objetivos da pesquisa
Avaliar, com apoio de voluntários com experiência em engenharia de software, a classificação humana do impacto de mudanças em casos de uso e das operações de manutenção de casos de teste em nível de sistema, para validar e comparar técnicas (incluindo abordagens assistidas por modelos de linguagem) no âmbito da tese de doutorado e de publicações científicas relacionadas (incluindo o experimento associado ao estudo ISSTA).

II) Justificativa e procedimentos (linguagem acessível)
A manutenção de casos de teste a partir de mudanças em requisitos é custosa e sujeita a erros. Nesta pesquisa, você acessará a plataforma Veredito pela internet, lerá este termo, informará dados básicos de perfil (nome, e-mail, habilidade profissional, instituição/empresa onde estuda ou trabalha, quando solicitado), responderá a uma pergunta de triagem e, se elegível, classificará itens de edição de casos de uso (impacto da mudança e operação de manutenção sugerida), podendo acrescentar comentários opcionais. Não há entrevista presencial nem gravação de áudio/vídeo.

III) Desconfortos, riscos e benefícios
Riscos / desconfortos: cansaço visual ou cognitivo ao rotular itens; desconforto leve ao ler textos técnicos; riscos típicos de ambiente virtual (falha de conexão, acesso indevido a contas de e-mail, limites tecnológicos para garantir confidencialidade absoluta).  
Mitigações: minimização de dados; pseudonimização das respostas de rotulagem; acesso autenticado por código enviado ao e-mail; servidor com controles de acesso; resultados científicos divulgados de forma agregada ou sob pseudônimo; menção nominal nos agradecimentos conforme escolha explícita (sim/não) na plataforma; possibilidade de interromper a qualquer momento.  
Benefícios: não há benefício individual direto garantido; benefício coletivo esperado é evidência científica para melhorar a manutenção de testes de software. Danos previsíveis serão evitados na medida do possível; eventuais danos decorrentes da pesquisa implicam direito a indenização conforme a legislação e as resoluções do CNS.

IV) Acompanhamento
Durante a coleta, o pesquisador poderá contatar você apenas pelos canais informados (e-mail) para esclarecimentos sobre a participação. Após o término, não há acompanhamento clínico; resultados poderão ser divulgados em artigos/tese. Se desejar ser informado(a) sobre os resultados gerais, escreva ao pesquisador responsável pelo e-mail indicado neste termo.

V) Liberdade de recusar ou interromper
Você pode recusar o convite, recusar o consentimento ou interromper a participação a qualquer momento, sem explicação e sem prejuízo. A revogação do consentimento não invalida tratamentos realizados anteriormente de forma legítima, observados os limites legais (LGPD) e a integridade do estudo.

VI) Sigilo e privacidade
Serão adotadas medidas para preservar sigilo e privacidade. As respostas de rotulagem são armazenadas sob pseudônimo e não são publicadas com seu nome. O e-mail não entra nas publicações. Nos agradecimentos de artigos, tese e trabalhos derivados, o primeiro e o último nome podem constar conforme a escolha explícita feita na plataforma Veredito (ver seção VII). Há, contudo, limites à confidencialidade total em meios eletrônicos (risco residual de violação), conforme Ofício Circular nº 02/2021.

VII) Divulgação dos resultados e agradecimentos
Os resultados científicos (análises e dados de rotulagem) serão divulgados em trabalhos acadêmicos de forma agregada ou sob pseudônimo, sem identificação direta nas tabelas/dados do estudo.

Nos agradecimentos do artigo e/ou da tese de doutorado do autor, a plataforma Veredito (fora deste texto) pede uma escolha explícita por botões de opção:
- Sim: desejo ter o meu primeiro e último nomes constando nos agradecimentos;
- Não: desejo permanecer anônimo(a) nos agradecimentos; nesse caso o sistema aplica a menção coletiva «Rotuladores Anonimizados por pedido [N]», em que N é a quantidade de pessoas que fizeram esse pedido, sem exigir que o administrador anonimize manualmente o participante só por essa preferência.
Em qualquer das duas opções, os dados de perfil e de rotulagem continuam sendo solicitados quando cabível, e a pessoa permanece apta a rotular se for elegível nas demais etapas.

VIII) Tratamento de dados pessoais (LGPD)
- Controlador: Universidade Federal de Campina Grande — Rua Aprígio Veloso, 882, Campina Grande–PB — CNPJ 05.055.128/0001-76.  
- Dados coletados: nome; e-mail; habilidade/experiência em engenharia de software; instituição/empresa (quando solicitado); escolha sobre constar nos agradecimentos (sim/não, na plataforma); respostas de triagem e de rotulagem; data/hora técnicas de uso da plataforma. Não se coletam dados sensíveis nos termos da LGPD.  
- Finalidades: administrar a participação; realizar a tarefa de rotulagem; analisar resultados; produzir tese/artigos (incluindo agradecimentos conforme a preferência informada); cumprir obrigações legais/institucionais; auditoria do estudo.  
- Base legal: consentimento do titular (Art. 7º, I, da LGPD), sem prejuízo de outras bases cabíveis para pesquisa.  
- Retenção: 5 (cinco) anos após o encerramento desta etapa da pesquisa; depois, eliminação ou anonimização, salvo obrigação legal.  
- Direitos: confirmação, acesso, correção, informação, anonimização/bloqueio/eliminação quando cabível, portabilidade quando regulamentada, revogação do consentimento, reclamação à instituição ou à ANPD — contato: diegoquirino@gmail.com.  
- Encarregado de proteção de dados institucional da UFCG: a definir; neste protocolo o contato operacional é o do pesquisador até indicação institucional.

IX) Registro do consentimento em ambiente virtual
O consentimento (aceite ou recusa) e a escolha sobre constar nos agradecimentos (sim/não) são registrados eletronicamente na plataforma Veredito, com carimbo de data/hora, versão e idioma do termo. Recomenda-se que você guarde uma cópia deste texto (download/impressão) para seus registros, conforme Ofício Circular nº 02/2021.

X) Gravação de voz e/ou imagem
Não haverá gravação de voz nem de imagem neste estudo; portanto não se solicita consentimento para gravação.

XI) Via do termo
Você terá acesso a este TCLE na plataforma e poderá salvar/imprimir uma via.

XII) Ressarcimento e indenização
A participação é voluntária e remota; não há previsão de despesas de deslocamento. Eventuais danos decorrentes da pesquisa implicam direito a indenização nos termos da legislação vigente e das normas do Sistema CEP/CONEP.

XIII) Contato do CEP
Caso me sinta prejudicado(a) por participar desta pesquisa, poderei recorrer ao Comitê de Ética em Pesquisa do Centro de Educação e Saúde da Universidade Federal de Campina Grande, localizado na Rua Profª. Maria Anita Furtado Coelho, S/N, Sítio Olho D’Água da Bica, Bloco: Central de Laboratórios de Análises Clínicas (LAC), 1º andar, Sala 16, CEP 58175-000, Cuité–PB, Tel.: (83) 3372-1835, E-mail: cep.ces.ufcg@gmail.com.

XIV) Contato do pesquisador
Carlos Diego Quirino Lima — PPGCC/CEEI/UFCG — e-mail diegoquirino@gmail.com — telefone (83) 98818-1876.



Consentimento: a decisão de participar ou não é registrada pelos botões da plataforma Veredito (aceitar / não concordar), e não por marcações neste texto.

Pesquisador responsável: Carlos Diego Quirino Lima  


Marcação interna: rascunho para submissão CEP — NÃO APROVADO PELO CEP. Coleta com participantes reais somente após aprovação.'
FROM consent_form WHERE version = 'fixture-1';

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'en-US', 'INFORMED CONSENT FORM - FIXTURE / DEV DRAFT - NOT APPROVED BY THE CEP

This is a translation; the Portuguese (Brazil) text is the reference version. This draft is for staging/dev-local only and was not approved by the research ethics committee (CEP). It mirrors the CEP-TCLE virtual-environment draft (Oficio Circular 02/2021, CNS 466/2012, LGPD).

Study title: Semantic Change Impact Analysis Framework for System-Level Test Case Maintenance
Responsible researcher: Carlos Diego Quirino Lima - Doctoral student PPGCC / CEEI / UFCG (student id 0123015808-15D) - Advisors: Wilkerson de Lucena Andrade; Everton Leandro Galdino Alves - diegoquirino@gmail.com - +55 83 98818-1876
Controller: Universidade Federal de Campina Grande - Rua Aprigio Veloso, 882, Campina Grande-PB - CNPJ 05.055.128/0001-76
CEP contact: cep.ces.ufcg@gmail.com / (83) 3372-1835

You are invited to take part entirely online via Veredito. You will read this form, provide profile data (name, e-mail, professional skill, organization/employer when asked), answer a screening question, and if eligible label use-case edits (change impact and maintenance operation). No audio/video recording. Participation is voluntary and may be withdrawn at any time.

Scientific results (labeling answers and analyses) are published aggregated or under a pseudonym. In acknowledgments of the author’s article and/or doctoral thesis, Veredito (outside this text) asks an explicit yes/no choice: Yes = list first and last names; No = remain anonymous, and the system applies the collective mention "Anonymized labelers by request [N]" (N = number of such requests) without requiring the administrator to anonymize the participant manually for that preference alone. Either choice still allows labeling if you are otherwise eligible; profile and labeling data are still requested when applicable. E-mail is not published.

Data processed (LGPD): name; e-mail; skill/experience; organization/employer when requested; acknowledgment yes/no choice set in the platform; screening and labeling answers; technical timestamps. No sensitive personal data under LGPD. Legal basis: consent (Art. 7, I). Retention: 5 years after this research stage ends, then deletion or anonymisation unless legally required.

Consent (agree / disagree) and the acknowledgment yes/no choice are recorded electronically in Veredito. By agreeing, you state that you read and understood this text and accept to take part voluntarily.'
FROM consent_form WHERE version = 'fixture-1';

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'es-ES', 'FORMULARIO DE CONSENTIMIENTO INFORMADO - FIXTURE / BORRADOR DEV - NO APROBADO POR EL CEP

Esta es una traducción; la versión de referencia es la portuguesa (Brasil). Este borrador es solo para staging/dev-local y no fue aprobado por el comité de ética (CEP). Refleja el borrador CEP-TCLE de ambiente virtual (Oficio Circular 02/2021, CNS 466/2012, LGPD).

Título: Semantic Change Impact Analysis Framework for System-Level Test Case Maintenance
Investigador: Carlos Diego Quirino Lima - Doctorando PPGCC / CEEI / UFCG (matrícula 0123015808-15D) - Orientadores: Wilkerson de Lucena Andrade; Everton Leandro Galdino Alves - diegoquirino@gmail.com - +55 83 98818-1876
Responsable: Universidade Federal de Campina Grande - CNPJ 05.055.128/0001-76
Contacto CEP: cep.ces.ufcg@gmail.com / (83) 3372-1835

Se le invita a participar en línea mediante Veredito: leer este término, indicar perfil (nombre, correo, habilidad profesional, institución/empresa cuando se solicite), responder a la triaje y, si es elegible, etiquetar ediciones de casos de uso. Sin grabación de audio/vídeo. Participación voluntaria, retirable en cualquier momento.

Los resultados científicos se publican de forma agregada o bajo seudónimo. En los agradecimientos del artículo y/o de la tesis doctoral del autor, Veredito (fuera de este texto) pide una elección explícita sí/no: Sí = figurar con nombre y apellido; No = permanecer anónimo(a), y el sistema aplica la mención colectiva "Rotuladores anonimizados por petición [N]" (N = cantidad de peticiones) sin exigir que el administrador anonimice manualmente al participante solo por esa preferencia. En ambos casos se puede etiquetar si se es elegible; los datos de perfil y de etiquetado siguen solicitándose cuando corresponda. El correo no se publica.

Datos (LGPD): nombre; correo; habilidad; institución/empresa cuando se solicite; elección sí/no sobre agradecimientos en la plataforma; respuestas de triaje y etiquetado; marcas temporales. Base legal: consentimiento (Art. 7, I). Retención: 5 años tras el cierre de esta etapa.

El consentimiento (aceptar / no aceptar) y la elección sí/no sobre agradecimientos se registran electrónicamente en Veredito. Al aceptar, declara que leyó este texto y acepta participar voluntariamente.'
FROM consent_form WHERE version = 'fixture-1';

