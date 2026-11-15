-- FIXTURE for local staging and automated tests. NOT the research consent form and NOT approved by
-- the research ethics committee (CEP). Never copy this file into db/migration: production must not
-- load it (spec 006, research section 3). The real text is a separate, researcher-supplied migration.
INSERT INTO consent_form (version, reference_language, created_at, published_at)
VALUES ('fixture-1', 'pt-BR', '2000-01-01 00:00:00.000000', '2000-01-01 00:00:00.000000');

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'pt-BR', 'TERMO DE CONSENTIMENTO LIVRE E ESCLARECIDO - FIXTURE - NÃO APROVADO PELO CEP

Este texto é apenas um exemplo para testes e para o ambiente de staging. Ele não é o termo da pesquisa e não foi aprovado pelo comitê de ética em pesquisa (CEP).

O estudo Veredito pede que você classifique edições de casos de uso. Suas respostas são armazenadas com um pseudônimo, e os dados são guardados no Google Cloud, em conformidade com a LGPD.

Ao concordar, você declara que leu este texto e aceita participar voluntariamente.'
FROM consent_form WHERE version = 'fixture-1';

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'en-US', 'INFORMED CONSENT FORM - FIXTURE - NOT APPROVED BY THE CEP

This is a translation; the reference version approved by the research ethics committee is the Portuguese (Brazil) one.

This text is only an example for tests and for the staging environment. It is not the research consent form and it was not approved by the research ethics committee (CEP).

The Veredito study asks you to label edits of use cases. Your answers are stored under a pseudonym, and the data is kept on Google Cloud, in compliance with the LGPD.

By agreeing, you state that you read this text and accept to take part voluntarily.'
FROM consent_form WHERE version = 'fixture-1';

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'es-ES', 'FORMULARIO DE CONSENTIMIENTO INFORMADO - FIXTURE - NO APROBADO POR EL CEP

Esta es una traducción; la versión de referencia aprobada por el comité de ética en investigación es la portuguesa (Brasil).

Este texto es solo un ejemplo para pruebas y para el entorno de staging. No es el formulario de la investigación y no fue aprobado por el comité de ética en investigación (CEP).

El estudio Veredito le pide clasificar ediciones de casos de uso. Sus respuestas se almacenan con un seudónimo y los datos se guardan en Google Cloud, de conformidad con la LGPD.

Al aceptar, usted declara que leyó este texto y acepta participar voluntariamente.'
FROM consent_form WHERE version = 'fixture-1';
