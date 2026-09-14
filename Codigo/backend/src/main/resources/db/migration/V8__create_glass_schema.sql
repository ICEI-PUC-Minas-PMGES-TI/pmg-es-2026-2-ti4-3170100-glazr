CREATE TABLE glasses (
    id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    thickness_mm NUMERIC(5, 2) NOT NULL,
    finish VARCHAR(80) NOT NULL,
    color VARCHAR(60) NOT NULL DEFAULT 'Incolor',
    price_per_square_meter NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_glasses PRIMARY KEY (id),
    CONSTRAINT ck_glasses_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_glasses_thickness_positive CHECK (thickness_mm > 0),
    CONSTRAINT ck_glasses_finish_not_blank CHECK (length(trim(finish)) > 0),
    CONSTRAINT ck_glasses_color_not_blank CHECK (length(trim(color)) > 0),
    CONSTRAINT ck_glasses_price_per_sqm_positive CHECK (price_per_square_meter > 0)
);

CREATE INDEX ix_glasses_name_ci ON glasses (lower(name));
CREATE INDEX ix_glasses_thickness_mm ON glasses (thickness_mm);
CREATE INDEX ix_glasses_active ON glasses (active);
CREATE UNIQUE INDEX uk_glasses_composite_ci ON glasses (lower(name), thickness_mm, lower(finish), lower(color));

INSERT INTO permissions (key, module, description) VALUES
    ('GLASS_READ', 'GLASS', 'Consultar vidros'),
    ('GLASS_CREATE', 'GLASS', 'Criar vidros'),
    ('GLASS_UPDATE', 'GLASS', 'Atualizar vidros'),
    ('GLASS_DELETE', 'GLASS', 'Excluir vidros')
ON CONFLICT (key) DO UPDATE SET
    module = EXCLUDED.module,
    description = EXCLUDED.description;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
SELECT '00000000-0000-0000-0000-000000000001', key FROM permissions WHERE module = 'GLASS'
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
VALUES ('00000000-0000-0000-0000-000000000002', 'GLASS_READ')
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;
