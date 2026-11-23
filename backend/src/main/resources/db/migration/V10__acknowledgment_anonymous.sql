-- Preference for paper acknowledgments: list first+last name unless this flag is true
-- (collective mention "Rotuladores Anonimizados por pedido [N]").
ALTER TABLE participant
    ADD COLUMN acknowledgment_anonymous BOOLEAN NOT NULL DEFAULT FALSE
        AFTER consent_decided_at;
