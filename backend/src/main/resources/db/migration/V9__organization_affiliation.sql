-- Spec 025: organization / employer affiliation on participant profile.
ALTER TABLE participant
    ADD COLUMN organization VARCHAR(16) NULL AFTER professional_skill_other,
    ADD COLUMN organization_other VARCHAR(100) NULL AFTER organization;

ALTER TABLE participant
    ADD CONSTRAINT ck_participant_organization CHECK (
        organization IS NULL OR organization IN ('tjpb', 'ufcg', 'other')),
    ADD CONSTRAINT ck_participant_organization_other CHECK (
        (organization = 'other' AND organization_other IS NOT NULL)
        OR ((organization IS NULL OR organization <> 'other') AND organization_other IS NULL));
