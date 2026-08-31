CREATE TABLE user_external_identities (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email_at_link VARCHAR(320) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ,
    CONSTRAINT pk_user_external_identities PRIMARY KEY (id),
    CONSTRAINT fk_user_external_identities_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_external_identities_provider_subject UNIQUE (provider, subject),
    CONSTRAINT uk_user_external_identities_user_provider UNIQUE (user_id, provider),
    CONSTRAINT ck_user_external_identities_provider CHECK (provider = 'GOOGLE'),
    CONSTRAINT ck_user_external_identities_subject_not_blank CHECK (length(trim(subject)) > 0)
);

CREATE INDEX ix_user_external_identities_user ON user_external_identities (user_id);

CREATE TABLE employee_invitations (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    delivery_status VARCHAR(20) NOT NULL,
    delivery_error_code VARCHAR(80),
    sent_at TIMESTAMPTZ,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_employee_invitations PRIMARY KEY (id),
    CONSTRAINT fk_employee_invitations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_employee_invitations_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT uk_employee_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_employee_invitations_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_employee_invitations_delivery_status
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_employee_invitations_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_employee_invitations_terminal_state
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE UNIQUE INDEX uk_employee_invitations_one_usable_per_user
    ON employee_invitations (user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX ix_employee_invitations_user_created
    ON employee_invitations (user_id, created_at DESC);

CREATE TABLE password_reset_requests (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_password_reset_requests PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_requests_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_password_reset_requests_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_password_reset_requests_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_password_reset_requests_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_requests_terminal_state
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE UNIQUE INDEX uk_password_reset_requests_one_usable_per_user
    ON password_reset_requests (user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX ix_password_reset_requests_user_created
    ON password_reset_requests (user_id, created_at DESC);

