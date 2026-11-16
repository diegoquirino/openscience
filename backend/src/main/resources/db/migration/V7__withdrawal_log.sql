CREATE TABLE withdrawal_log (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    occurred_at                DATETIME(6)  NOT NULL,
    pseudonym                  VARCHAR(16)  NOT NULL,
    option_code                VARCHAR(20)  NOT NULL,
    email_sent                 BOOLEAN      NOT NULL,
    final_responses_discarded  INT          NOT NULL,
    tasks_returned             INT          NOT NULL,
    participant_id             BIGINT       NULL,
    CONSTRAINT pk_withdrawal_log PRIMARY KEY (id),
    CONSTRAINT fk_withdrawal_log_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
    CONSTRAINT ck_withdrawal_log_option CHECK (option_code IN ('keep_responses', 'discard_responses')),
    CONSTRAINT ck_withdrawal_log_counts CHECK (
        final_responses_discarded >= 0 AND tasks_returned >= 0),
    INDEX ix_withdrawal_log_occurred_at (occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
