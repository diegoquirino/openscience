-- Spec 004: sign in with name, e-mail and code (specs/004-email-code-access/data-model.md).
-- Only additions, so the revision of spec 003 keeps working after a rollback.

ALTER TABLE config ADD COLUMN admin_name VARCHAR(120) NULL AFTER admin_email;

ALTER TABLE access_code ADD COLUMN name VARCHAR(120) NULL AFTER email;

CREATE INDEX ix_access_code_created_at ON access_code (created_at);

CREATE INDEX ix_access_session_expires_at ON access_session (expires_at);
