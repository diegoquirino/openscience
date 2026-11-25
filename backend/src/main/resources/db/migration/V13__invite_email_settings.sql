-- Spec 031: invite e-mail settings (date+time) and editable templates; participant send markers.
ALTER TABLE config
    ADD COLUMN invite_caae VARCHAR(64) NULL AFTER updated_at,
    ADD COLUMN invite_platform_url VARCHAR(500) NOT NULL DEFAULT 'https://diegoquirino.cloud' AFTER invite_caae,
    ADD COLUMN invite_collection_start_at DATETIME(6) NULL AFTER invite_platform_url,
    ADD COLUMN invite_collection_end_at DATETIME(6) NULL AFTER invite_collection_start_at,
    ADD COLUMN invite_reminder_at DATETIME(6) NULL AFTER invite_collection_end_at;

UPDATE config
SET invite_caae = 'CAAE [número]',
    invite_platform_url = 'https://diegoquirino.cloud',
    invite_collection_start_at = '2026-12-21 03:00:00.000000',
    invite_collection_end_at = '2027-01-09 02:59:00.000000',
    invite_reminder_at = '2026-12-28 15:00:00.000000'
WHERE id = 1;

CREATE TABLE email_template (
    kind       VARCHAR(32)  NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       MEDIUMTEXT   NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_email_template PRIMARY KEY (kind),
    CONSTRAINT ck_email_template_kind CHECK (kind IN ('inicial', 'lembrete', 'agradecimento'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO email_template (kind, subject, body, updated_at) VALUES
('inicial', 'Convite para uma pesquisa curta sobre manutenção de testes (voluntária)', 'Olá, {{nome}}!

Meu nome é Carlos Diego Quirino Lima, sou doutorando do PPGCC/UFCG, e gostaria de convidar você a participar, de forma totalmente voluntária, de uma pesquisa online sobre manutenção de casos de teste a partir de mudanças em casos de uso.

A participação é pela plataforma Veredito: {{url}}

1. Você lê o Termo de Consentimento Livre e Esclarecido (TCLE) e decide se quer participar (pode recusar sem nenhum problema).
2. Se aceitar, entra com um código enviado ao seu e-mail, preenche um perfil rápido e responde a uma pergunta de triagem.
3. Se for elegível, classifica alguns itens (impacto da mudança e operação de manutenção sugerida). Dá para pausar ou sair a qualquer momento.

Não há entrevista nem gravação de áudio ou vídeo. Os dados são tratados sob pseudônimo nas análises e, nos agradecimentos de artigos e da tese, você escolhe se quer constar com seu primeiro e último nome ou permanecer anônimo(a).

A pesquisa foi aprovada pelo Comitê de Ética em Pesquisa do HUAC/UFCG ({{caae}}). Período de coleta: {{coleta_inicio}} a {{coleta_fim}}. Dúvidas sobre a pesquisa: responda este e-mail ou ligue (83) 98818-1876. Dúvidas sobre seus direitos como participante: CEP/HUAC/UFCG, e-mail cep@huac.ufcg.edu.br (ou huaccep@gmail.com), telefone (83) 2101-5545, de segunda a sexta, 8h–12h e 14h–17h.

Muito obrigado desde já! Sua experiência em engenharia de software e testes faz muita diferença para o estudo.

Um abraço,
Carlos Diego Quirino Lima
Doutorando – PPGCC/CEEI/UFCG
diegoquirino@gmail.com | (83) 98818-1876
Orientação: Prof. Dr. Wilkerson de Lucena Andrade | Coorientação: Prof. Dr. Everton Leandro Galdino Alves', UTC_TIMESTAMP(6)),
('lembrete', 'Lembrete gentil: convite da pesquisa Veredito (sem pressão!)', 'Olá, {{nome}}!

Passando só para lembrar, com todo carinho e sem nenhuma pressão, do convite para a pesquisa online na plataforma Veredito:

{{url}}

Se já entrou e concluiu (ou decidiu não participar), pode desconsiderar este e-mail — está tudo certo.

Se ainda não teve tempo: o fluxo começa pelo Termo de Consentimento Livre e Esclarecido (TCLE) — você lê e só segue se quiser. Se aceitar, entra com um código enviado ao seu e-mail. A tarefa é objetiva, e você pode pausar ou sair quando preferir. A coleta vai até {{coleta_fim}}.

A pesquisa foi aprovada pelo Comitê de Ética em Pesquisa do HUAC/UFCG ({{caae}}). Dúvidas sobre a pesquisa: responda por aqui ou ligue (83) 98818-1876. Dúvidas sobre seus direitos como participante: CEP/HUAC/UFCG, e-mail cep@huac.ufcg.edu.br (ou huaccep@gmail.com), telefone (83) 2101-5545, de segunda a sexta, 8h–12h e 14h–17h.

Obrigado de novo pela atenção!

Um abraço,
Carlos Diego Quirino Lima
Doutorando – PPGCC/CEEI/UFCG
diegoquirino@gmail.com | (83) 98818-1876', UTC_TIMESTAMP(6)),
('agradecimento', 'Muito obrigado por participar da pesquisa Veredito', 'Olá, {{nome}}!

Só queria agradecer de coração pela sua participação na pesquisa na Veredito. Seu tempo e o cuidado ao rotular os itens fazem toda a diferença para o estudo (e para a tese).

Nos agradecimentos de artigos e da tese, vale a escolha que você fez na plataforma: se escolheu "sim", constarão seu primeiro e último nome; se escolheu "não", você permanece anônimo(a) e entra apenas na menção coletiva «Rotuladores Anonimizados por pedido [N]».

Seus dados de rotulagem seguem tratados sob pseudônimo e os resultados serão divulgados de forma agregada. Se quiser saber dos resultados gerais no futuro, é só me escrever: diegoquirino@gmail.com. Você também pode, a qualquer momento, retirar seu consentimento ou exercer seus direitos sobre seus dados (LGPD) pelo mesmo e-mail.

Dúvidas sobre seus direitos como participante: CEP/HUAC/UFCG ({{caae}}), e-mail cep@huac.ufcg.edu.br (ou huaccep@gmail.com), telefone (83) 2101-5545, de segunda a sexta, 8h–12h e 14h–17h.

Um abraço e obrigado de novo!

Carlos Diego Quirino Lima
Doutorando – PPGCC/CEEI/UFCG
diegoquirino@gmail.com | (83) 98818-1876', UTC_TIMESTAMP(6));


ALTER TABLE participant
    ADD COLUMN invite_inicial_sent_at DATETIME(6) NULL,
    ADD COLUMN invite_lembrete_sent_at DATETIME(6) NULL,
    ADD COLUMN invite_agradecimento_sent_at DATETIME(6) NULL;
