CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_failed_login_attempts_non_negative CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX uk_users_email_ci ON users (lower(email));
CREATE INDEX ix_users_active ON users (active);
CREATE INDEX ix_users_display_name_ci ON users (lower(display_name));

CREATE TABLE access_profiles (
    id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL,
    system_profile BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_access_profiles PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_access_profiles_name_ci ON access_profiles (lower(name));
CREATE INDEX ix_access_profiles_active ON access_profiles (active);

CREATE TABLE permissions (
    key VARCHAR(80) NOT NULL,
    module VARCHAR(60) NOT NULL,
    description VARCHAR(300) NOT NULL,
    CONSTRAINT pk_permissions PRIMARY KEY (key)
);

CREATE TABLE user_access_profiles (
    user_id UUID NOT NULL,
    access_profile_id UUID NOT NULL,
    CONSTRAINT pk_user_access_profiles PRIMARY KEY (user_id, access_profile_id),
    CONSTRAINT fk_user_access_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_access_profiles_profile FOREIGN KEY (access_profile_id) REFERENCES access_profiles (id)
);

CREATE INDEX ix_user_access_profiles_profile ON user_access_profiles (access_profile_id, user_id);

CREATE TABLE access_profile_permissions (
    access_profile_id UUID NOT NULL,
    permission_key VARCHAR(80) NOT NULL,
    CONSTRAINT pk_access_profile_permissions PRIMARY KEY (access_profile_id, permission_key),
    CONSTRAINT fk_access_profile_permissions_profile FOREIGN KEY (access_profile_id) REFERENCES access_profiles (id),
    CONSTRAINT fk_access_profile_permissions_permission FOREIGN KEY (permission_key) REFERENCES permissions (key)
);

CREATE INDEX ix_access_profile_permissions_permission ON access_profile_permissions (permission_key, access_profile_id);
