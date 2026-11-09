CREATE TABLE quotes (
    id UUID NOT NULL,
    quote_number BIGSERIAL NOT NULL,
    customer_name VARCHAR(120) NOT NULL,
    customer_phone VARCHAR(30),
    customer_email VARCHAR(120),
    customer_address VARCHAR(250),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(500),
    labor_cost NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    freight_cost NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_quotes PRIMARY KEY (id),
    CONSTRAINT ck_quotes_customer_name_not_blank CHECK (length(trim(customer_name)) > 0),
    CONSTRAINT ck_quotes_status_valid CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_quotes_labor_cost_non_negative CHECK (labor_cost >= 0),
    CONSTRAINT ck_quotes_freight_cost_non_negative CHECK (freight_cost >= 0),
    CONSTRAINT ck_quotes_total_amount_non_negative CHECK (total_amount >= 0)
);

CREATE INDEX ix_quotes_customer_name_ci ON quotes (lower(customer_name));
CREATE INDEX ix_quotes_status ON quotes (status);
CREATE INDEX ix_quotes_created_at ON quotes (created_at DESC);

CREATE TABLE quote_glass_items (
    id UUID NOT NULL,
    quote_id UUID NOT NULL,
    glass_id UUID NOT NULL,
    width_mm NUMERIC(10, 2) NOT NULL,
    height_mm NUMERIC(10, 2) NOT NULL,
    quantity INT NOT NULL,
    unit_area_m2 NUMERIC(14, 6) NOT NULL,
    total_area_m2 NUMERIC(14, 6) NOT NULL,
    price_per_square_meter NUMERIC(12, 2) NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    notes VARCHAR(300),
    CONSTRAINT pk_quote_glass_items PRIMARY KEY (id),
    CONSTRAINT fk_quote_glass_items_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_glass_items_glass FOREIGN KEY (glass_id) REFERENCES glasses (id),
    CONSTRAINT ck_quote_glass_width_positive CHECK (width_mm > 0),
    CONSTRAINT ck_quote_glass_height_positive CHECK (height_mm > 0),
    CONSTRAINT ck_quote_glass_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_quote_glass_total_price_non_negative CHECK (total_price >= 0)
);

CREATE INDEX ix_quote_glass_items_quote_id ON quote_glass_items (quote_id);

CREATE TABLE quote_kit_items (
    id UUID NOT NULL,
    quote_id UUID NOT NULL,
    kit_id UUID NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    notes VARCHAR(300),
    CONSTRAINT pk_quote_kit_items PRIMARY KEY (id),
    CONSTRAINT fk_quote_kit_items_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_kit_items_kit FOREIGN KEY (kit_id) REFERENCES kits (id),
    CONSTRAINT ck_quote_kit_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_quote_kit_total_price_non_negative CHECK (total_price >= 0)
);

CREATE INDEX ix_quote_kit_items_quote_id ON quote_kit_items (quote_id);

INSERT INTO permissions (key, module, description) VALUES
    ('QUOTE_READ', 'QUOTE', 'Consultar orçamentos e pedidos'),
    ('QUOTE_CREATE', 'QUOTE', 'Criar orçamentos'),
    ('QUOTE_UPDATE', 'QUOTE', 'Atualizar orçamentos'),
    ('QUOTE_STATUS_UPDATE', 'QUOTE', 'Alterar status de orçamentos e pedidos'),
    ('QUOTE_DELETE', 'QUOTE', 'Excluir orçamentos')
ON CONFLICT (key) DO UPDATE SET
    module = EXCLUDED.module,
    description = EXCLUDED.description;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
SELECT '00000000-0000-0000-0000-000000000001', key FROM permissions WHERE module = 'QUOTE'
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'QUOTE_READ'),
    ('00000000-0000-0000-0000-000000000002', 'QUOTE_CREATE'),
    ('00000000-0000-0000-0000-000000000002', 'QUOTE_UPDATE'),
    ('00000000-0000-0000-0000-000000000002', 'QUOTE_STATUS_UPDATE')
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;
