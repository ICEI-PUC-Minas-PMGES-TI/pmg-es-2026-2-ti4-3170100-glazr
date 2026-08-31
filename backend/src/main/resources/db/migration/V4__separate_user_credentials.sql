ALTER TABLE users ADD COLUMN account_type VARCHAR(30);

UPDATE users SET account_type = 'EMPLOYEE' WHERE account_type IS NULL;

ALTER TABLE users ALTER COLUMN account_type SET NOT NULL;
ALTER TABLE users ALTER COLUMN account_type SET DEFAULT 'EMPLOYEE';
ALTER TABLE users ADD CONSTRAINT ck_users_account_type
    CHECK (account_type IN ('EMPLOYEE', 'CUSTOMER'));

CREATE TABLE user_password_credentials (
    user_id UUID NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_password_credentials PRIMARY KEY (user_id),
    CONSTRAINT fk_user_password_credentials_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_password_credentials_hash_not_blank CHECK (length(trim(password_hash)) > 0)
);

INSERT INTO user_password_credentials (
    user_id, password_hash, password_changed_at, created_at, updated_at
)
SELECT id, password_hash, updated_at, created_at, updated_at
FROM users;

DO $$
BEGIN
    IF (SELECT count(*) FROM users) <> (SELECT count(*) FROM user_password_credentials) THEN
        RAISE EXCEPTION 'password credential migration did not copy every user';
    END IF;
END $$;

ALTER TABLE users DROP COLUMN password_hash;

