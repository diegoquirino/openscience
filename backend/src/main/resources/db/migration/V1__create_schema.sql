-- Schema of Veredito (specs/003-deployment-foundation/data-model.md).
-- Final after the first production deployment: change the schema only in new migrations.
-- `system` is a reserved word in MySQL 8+ and is always quoted.

CREATE TABLE import_batch (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    file_name      VARCHAR(255) NOT NULL,
    imported_at    DATETIME(6)  NOT NULL,
    included_count INT          NOT NULL,
    ignored_count  INT          NOT NULL,
    error_count    INT          NOT NULL,
    CONSTRAINT pk_import_batch PRIMARY KEY (id),
    CONSTRAINT ck_import_batch_counts CHECK (included_count >= 0 AND ignored_count >= 0 AND error_count >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE block (
    id       BIGINT  NOT NULL AUTO_INCREMENT,
    batch_id BIGINT  NOT NULL,
    number   INT     NOT NULL,
    size     TINYINT NOT NULL,
    CONSTRAINT pk_block PRIMARY KEY (id),
    CONSTRAINT uq_block_number UNIQUE (number),
    CONSTRAINT fk_block_batch FOREIGN KEY (batch_id) REFERENCES import_batch (id),
    CONSTRAINT ck_block_size CHECK (size >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE item (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    `system`       VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    edit_key       VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    uc             VARCHAR(255) NOT NULL,
    kind           VARCHAR(4)   NOT NULL,
    field          VARCHAR(100) NOT NULL,
    origin         TEXT         NULL,
    target         TEXT         NULL,
    origin_version VARCHAR(20)  NOT NULL,
    target_version VARCHAR(20)  NOT NULL,
    origin_usecase MEDIUMTEXT   NOT NULL,
    target_usecase MEDIUMTEXT   NOT NULL,
    batch_id       BIGINT       NOT NULL,
    block_id       BIGINT       NOT NULL,
    position       TINYINT      NOT NULL,
    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT uq_item_system_edit_key UNIQUE (`system`, edit_key),
    CONSTRAINT uq_item_block_position UNIQUE (block_id, position),
    CONSTRAINT fk_item_batch FOREIGN KEY (batch_id) REFERENCES import_batch (id),
    CONSTRAINT fk_item_block FOREIGN KEY (block_id) REFERENCES block (id),
    CONSTRAINT ck_item_kind CHECK (kind IN ('ADD', 'DEL', 'MOD', 'META')),
    CONSTRAINT ck_item_origin_or_target CHECK (origin IS NOT NULL OR target IS NOT NULL),
    CONSTRAINT ck_item_position CHECK (position >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE consent_form (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    version            VARCHAR(20) NOT NULL,
    reference_language CHAR(5)     NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    published_at       DATETIME(6) NULL,
    CONSTRAINT pk_consent_form PRIMARY KEY (id),
    CONSTRAINT uq_consent_form_version UNIQUE (version),
    CONSTRAINT ck_consent_form_reference_language CHECK (reference_language = 'pt-BR')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE consent_form_text (
    id              BIGINT     NOT NULL AUTO_INCREMENT,
    consent_form_id BIGINT     NOT NULL,
    language        CHAR(5)    NOT NULL,
    text            MEDIUMTEXT NOT NULL,
    CONSTRAINT pk_consent_form_text PRIMARY KEY (id),
    CONSTRAINT uq_consent_form_text_language UNIQUE (consent_form_id, language),
    CONSTRAINT fk_consent_form_text_form FOREIGN KEY (consent_form_id) REFERENCES consent_form (id),
    CONSTRAINT ck_consent_form_text_language CHECK (language IN ('pt-BR', 'en-US', 'es-ES'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE participant (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    pseudonym                VARCHAR(16)  NOT NULL,
    name                     VARCHAR(120) NULL,
    email                    VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    registered_at            DATETIME(6)  NOT NULL,
    preferred_language       CHAR(5)      NULL,
    consent_form_id          BIGINT       NULL,
    consent_decision         VARCHAR(8)   NULL,
    consent_language         CHAR(5)      NULL,
    consent_decided_at       DATETIME(6)  NULL,
    professional_skill       VARCHAR(32)  NULL,
    professional_skill_other VARCHAR(100) NULL,
    screening_answer         VARCHAR(16)  NULL,
    eligible                 BOOLEAN      NULL,
    anonymized_at            DATETIME(6)  NULL,
    CONSTRAINT pk_participant PRIMARY KEY (id),
    CONSTRAINT uq_participant_pseudonym UNIQUE (pseudonym),
    CONSTRAINT uq_participant_email UNIQUE (email),
    CONSTRAINT fk_participant_consent_form FOREIGN KEY (consent_form_id) REFERENCES consent_form (id),
    CONSTRAINT ck_participant_preferred_language CHECK (preferred_language IN ('pt-BR', 'en-US', 'es-ES')),
    CONSTRAINT ck_participant_consent_language CHECK (consent_language IN ('pt-BR', 'en-US', 'es-ES')),
    CONSTRAINT ck_participant_consent_decision CHECK (consent_decision IN ('accepted', 'refused')),
    CONSTRAINT ck_participant_consent_complete CHECK (
        (consent_form_id IS NULL AND consent_decision IS NULL AND consent_language IS NULL AND consent_decided_at IS NULL)
        OR (consent_form_id IS NOT NULL AND consent_decision IS NOT NULL AND consent_language IS NOT NULL
            AND consent_decided_at IS NOT NULL)),
    CONSTRAINT ck_participant_professional_skill CHECK (professional_skill IN (
        'developer', 'software_engineer', 'systems_analyst', 'requirements_analyst', 'test_analyst',
        'test_engineer', 'software_architect', 'tech_lead', 'product_project_manager',
        'professor_researcher', 'graduate_student', 'undergraduate_student', 'other')),
    CONSTRAINT ck_participant_professional_skill_other CHECK (
        professional_skill_other IS NULL OR professional_skill = 'other'),
    CONSTRAINT ck_participant_screening_answer CHECK (screening_answer IN ('yes_professional', 'yes_academic', 'no')),
    CONSTRAINT ck_participant_anonymization CHECK (
        (anonymized_at IS NULL AND name IS NOT NULL AND email IS NOT NULL)
        OR (anonymized_at IS NOT NULL AND name IS NULL AND email IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE task (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    block_id       BIGINT      NOT NULL,
    role           TINYINT     NOT NULL,
    queue_position INT         NOT NULL,
    status         VARCHAR(9)  NOT NULL DEFAULT 'free',
    participant_id BIGINT      NULL,
    reserved_at    DATETIME(6) NULL,
    expires_at     DATETIME(6) NULL,
    submitted_at   DATETIME(6) NULL,
    CONSTRAINT pk_task PRIMARY KEY (id),
    CONSTRAINT uq_task_queue_position UNIQUE (queue_position),
    CONSTRAINT uq_task_block_role UNIQUE (block_id, role),
    CONSTRAINT uq_task_block_participant UNIQUE (block_id, participant_id),
    CONSTRAINT fk_task_block FOREIGN KEY (block_id) REFERENCES block (id),
    CONSTRAINT fk_task_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
    CONSTRAINT ck_task_role CHECK (role IN (1, 2)),
    CONSTRAINT ck_task_status CHECK (status IN ('free', 'reserved', 'completed')),
    CONSTRAINT ck_task_state CHECK (
        (status = 'free' AND participant_id IS NULL AND reserved_at IS NULL AND expires_at IS NULL
            AND submitted_at IS NULL)
        OR (status = 'reserved' AND participant_id IS NOT NULL AND reserved_at IS NOT NULL
            AND expires_at IS NOT NULL AND submitted_at IS NULL)
        OR (status = 'completed' AND participant_id IS NOT NULL AND reserved_at IS NOT NULL
            AND submitted_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE response (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    task_id               BIGINT        NOT NULL,
    item_id               BIGINT        NOT NULL,
    participant_id        BIGINT        NOT NULL,
    state                 VARCHAR(5)    NOT NULL,
    change_impact         VARCHAR(4)    NOT NULL,
    maintenance_operation VARCHAR(8)    NOT NULL,
    comment               VARCHAR(1000) NULL,
    language              CHAR(5)       NOT NULL,
    saved_at              DATETIME(6)   NOT NULL,
    CONSTRAINT pk_response PRIMARY KEY (id),
    CONSTRAINT uq_response_task_item UNIQUE (task_id, item_id),
    CONSTRAINT fk_response_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_response_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_response_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
    CONSTRAINT ck_response_state CHECK (state IN ('draft', 'final')),
    CONSTRAINT ck_response_change_impact CHECK (change_impact IN ('LOW', 'HIGH')),
    CONSTRAINT ck_response_maintenance_operation CHECK (maintenance_operation IN (
        'KEEP', 'UPDATE', 'REMOVE', 'CREATE', 'MERGE', 'SPLIT', 'REASSIGN', 'FLAG')),
    CONSTRAINT ck_response_language CHECK (language IN ('pt-BR', 'en-US', 'es-ES'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE access_code (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    code_hash  VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    attempts   TINYINT      NOT NULL DEFAULT 0,
    used_at    DATETIME(6)  NULL,
    CONSTRAINT pk_access_code PRIMARY KEY (id),
    INDEX ix_access_code_email_created_at (email, created_at),
    CONSTRAINT ck_access_code_attempts CHECK (attempts >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE access_session (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    token_hash     VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    subject        VARCHAR(13)  NOT NULL,
    participant_id BIGINT       NULL,
    created_at     DATETIME(6)  NOT NULL,
    expires_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_access_session PRIMARY KEY (id),
    CONSTRAINT uq_access_session_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_access_session_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
    CONSTRAINT ck_access_session_subject CHECK (subject IN ('participant', 'administrator')),
    CONSTRAINT ck_access_session_participant CHECK ((subject = 'participant') = (participant_id IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE config (
    id                       TINYINT      NOT NULL,
    admin_email              VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    admin_preferred_language CHAR(5)      NULL,
    reservation_minutes      SMALLINT     NOT NULL,
    block_size               TINYINT      NOT NULL,
    daily_email_limit        SMALLINT     NOT NULL,
    study_phase              VARCHAR(10)  NOT NULL,
    study_phase_changed_at   DATETIME(6)  NULL,
    updated_at               DATETIME(6)  NOT NULL,
    CONSTRAINT pk_config PRIMARY KEY (id),
    CONSTRAINT ck_config_single_row CHECK (id = 1),
    CONSTRAINT ck_config_admin_preferred_language CHECK (admin_preferred_language IN ('pt-BR', 'en-US', 'es-ES')),
    CONSTRAINT ck_config_reservation_minutes CHECK (reservation_minutes BETWEEN 1 AND 240),
    CONSTRAINT ck_config_block_size CHECK (block_size BETWEEN 1 AND 50),
    CONSTRAINT ck_config_daily_email_limit CHECK (daily_email_limit BETWEEN 0 AND 2000),
    CONSTRAINT ck_config_study_phase CHECK (study_phase IN ('pilot', 'collection'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
