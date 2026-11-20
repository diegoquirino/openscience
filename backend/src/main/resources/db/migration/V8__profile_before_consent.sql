-- Spec 013: name may be null until the profile step; e-mail still required when not anonymized.
ALTER TABLE participant DROP CHECK ck_participant_anonymization;
ALTER TABLE participant ADD CONSTRAINT ck_participant_anonymization CHECK (
    (anonymized_at IS NULL AND email IS NOT NULL)
    OR (anonymized_at IS NOT NULL AND name IS NULL AND email IS NULL)
);
