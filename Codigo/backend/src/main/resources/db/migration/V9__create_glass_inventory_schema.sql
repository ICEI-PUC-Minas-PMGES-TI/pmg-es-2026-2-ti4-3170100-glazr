CREATE TABLE glass_stock_settings (
    glass_id UUID NOT NULL,
    minimum_area_m2 NUMERIC(14, 6),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_glass_stock_settings PRIMARY KEY (glass_id),
    CONSTRAINT fk_glass_stock_settings_glass FOREIGN KEY (glass_id) REFERENCES glasses (id),
    CONSTRAINT ck_glass_stock_settings_minimum_non_negative
        CHECK (minimum_area_m2 IS NULL OR minimum_area_m2 >= 0)
);

CREATE TABLE glass_sheets (
    id UUID NOT NULL,
    glass_id UUID NOT NULL,
    original_width_mm NUMERIC(10, 2) NOT NULL,
    original_height_mm NUMERIC(10, 2) NOT NULL,
    original_area_m2 NUMERIC(14, 6) NOT NULL,
    remaining_area_m2 NUMERIC(14, 6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_glass_sheets PRIMARY KEY (id),
    CONSTRAINT fk_glass_sheets_glass FOREIGN KEY (glass_id) REFERENCES glasses (id),
    CONSTRAINT ck_glass_sheets_width_positive CHECK (original_width_mm > 0),
    CONSTRAINT ck_glass_sheets_height_positive CHECK (original_height_mm > 0),
    CONSTRAINT ck_glass_sheets_original_area_positive CHECK (original_area_m2 > 0),
    CONSTRAINT ck_glass_sheets_remaining_area_valid
        CHECK (remaining_area_m2 >= 0 AND remaining_area_m2 <= original_area_m2),
    CONSTRAINT ck_glass_sheets_status CHECK (status IN ('AVAILABLE', 'DEPLETED'))
);

CREATE INDEX ix_glass_sheets_glass_status ON glass_sheets (glass_id, status);

CREATE TABLE stock_movements (
    id UUID NOT NULL,
    glass_sheet_id UUID NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    previous_remaining_area_m2 NUMERIC(14, 6),
    resulting_remaining_area_m2 NUMERIC(14, 6) NOT NULL,
    piece_width_mm NUMERIC(10, 2),
    piece_height_mm NUMERIC(10, 2),
    piece_area_m2 NUMERIC(14, 6),
    loss_area_m2 NUMERIC(14, 6),
    notes VARCHAR(300),
    performed_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_sheet FOREIGN KEY (glass_sheet_id) REFERENCES glass_sheets (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (performed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('RECEIPT', 'ADJUSTMENT', 'MANUAL_CUT')),
    CONSTRAINT ck_stock_movements_previous_non_negative
        CHECK (previous_remaining_area_m2 IS NULL OR previous_remaining_area_m2 >= 0),
    CONSTRAINT ck_stock_movements_result_non_negative CHECK (resulting_remaining_area_m2 >= 0),
    CONSTRAINT ck_stock_movements_piece_width_positive
        CHECK (piece_width_mm IS NULL OR piece_width_mm > 0),
    CONSTRAINT ck_stock_movements_piece_height_positive
        CHECK (piece_height_mm IS NULL OR piece_height_mm > 0),
    CONSTRAINT ck_stock_movements_piece_area_non_negative
        CHECK (piece_area_m2 IS NULL OR piece_area_m2 >= 0),
    CONSTRAINT ck_stock_movements_loss_area_non_negative
        CHECK (loss_area_m2 IS NULL OR loss_area_m2 >= 0)
);

CREATE INDEX ix_stock_movements_sheet_created_at ON stock_movements (glass_sheet_id, created_at DESC);
