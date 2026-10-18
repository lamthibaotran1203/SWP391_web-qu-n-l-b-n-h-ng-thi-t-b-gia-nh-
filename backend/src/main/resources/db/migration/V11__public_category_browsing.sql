-- UC-8.3: versions 8 through 10 already exist on the shared database.
-- Preserve existing categories as active; publication is independent of products.
ALTER TABLE app.hm_category
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN image_url varchar(2000),
    ADD CONSTRAINT ck_category_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
CREATE INDEX ix_category_public_parent ON app.hm_category(parent_id, id)
    WHERE status='ACTIVE' AND NOT is_deleted;
COMMENT ON COLUMN app.hm_sku.selling_price IS
    'Final VAT-inclusive retail price in whole VND, configured by management. Do not add VAT again.';
