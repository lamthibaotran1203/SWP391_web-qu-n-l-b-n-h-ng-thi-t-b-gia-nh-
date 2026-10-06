CREATE TABLE app.email_verification_token (
    token_id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMPTZ NOT NULL,

    used_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id)
        REFERENCES app.user_account(user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_user
    ON app.email_verification_token(user_id);

CREATE INDEX idx_email_verification_expires
    ON app.email_verification_token(expires_at);