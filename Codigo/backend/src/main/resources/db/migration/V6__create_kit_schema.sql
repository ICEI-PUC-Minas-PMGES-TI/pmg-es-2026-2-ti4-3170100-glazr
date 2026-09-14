CREATE TABLE kits (
    id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    color VARCHAR(32) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_kits PRIMARY KEY (id),
    CONSTRAINT ck_kits_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_kits_color_not_blank CHECK (length(trim(color)) > 0),
    CONSTRAINT ck_kits_price_non_negative CHECK (price >= 0)
);

CREATE UNIQUE INDEX uk_kits_name_ci ON kits (lower(name));

CREATE TABLE kit_components (
    id UUID NOT NULL,
    kit_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL,
    notes VARCHAR(300),
    CONSTRAINT pk_kit_components PRIMARY KEY (id),
    CONSTRAINT fk_kit_components_kit FOREIGN KEY (kit_id)
        REFERENCES kits (id) ON DELETE CASCADE,
    CONSTRAINT ck_kit_components_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_kit_components_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_kit_components_kit ON kit_components (kit_id);

INSERT INTO permissions (key, module, description) VALUES
    ('KIT_READ', 'KIT', 'Consultar kits'),
    ('KIT_CREATE', 'KIT', 'Criar kits'),
    ('KIT_UPDATE', 'KIT', 'Atualizar kits'),
    ('KIT_DELETE', 'KIT', 'Excluir kits')
ON CONFLICT (key) DO UPDATE SET
    module = EXCLUDED.module,
    description = EXCLUDED.description;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
SELECT '00000000-0000-0000-0000-000000000001', key FROM permissions
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;