CREATE TABLE import_preview (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    file_name         VARCHAR(255) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    expires_at        DATETIME(6)  NOT NULL,
    included_count    INT          NOT NULL,
    ignored_count     INT          NOT NULL,
    error_count       INT          NOT NULL,
    block_count       INT          NOT NULL,
    block_size        TINYINT      NOT NULL,
    extra_columns_json TEXT        NOT NULL,
    included_json     MEDIUMTEXT   NOT NULL,
    ignored_json      MEDIUMTEXT   NOT NULL,
    errors_json       MEDIUMTEXT   NOT NULL,
    block_sizes_json  VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_import_preview_counts CHECK (
        included_count >= 0 AND ignored_count >= 0 AND error_count >= 0 AND block_count >= 0
    )
);

CREATE INDEX ix_import_preview_expires_at ON import_preview (expires_at);
