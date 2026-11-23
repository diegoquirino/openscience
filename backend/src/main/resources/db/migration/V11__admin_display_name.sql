-- Spec 026: default administrator display name (FR-050 identity) when still unset.
-- E-mail-only sign-in (013) does not collect a name on the access code for the admin.
UPDATE config
SET admin_name = 'Carlos Diego Quirino Lima'
WHERE id = 1
  AND (admin_name IS NULL OR TRIM(admin_name) = '');
