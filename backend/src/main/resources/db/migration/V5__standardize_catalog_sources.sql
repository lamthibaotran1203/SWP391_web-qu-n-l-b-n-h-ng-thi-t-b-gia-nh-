-- Historical V2 fields are preserved for audit, not live price/stock/code sources.
ALTER TABLE app.product RENAME COLUMN sku TO legacy_sku;
ALTER TABLE app.product RENAME COLUMN price TO legacy_price;
ALTER TABLE app.product RENAME COLUMN stock_quantity TO legacy_stock_quantity;
ALTER TABLE app.product RENAME COLUMN category TO legacy_category;
ALTER TABLE app.product RENAME COLUMN brand TO legacy_brand;
ALTER TABLE app.product
    ALTER COLUMN legacy_sku DROP NOT NULL,
    ALTER COLUMN legacy_price DROP NOT NULL,
    ALTER COLUMN legacy_stock_quantity DROP NOT NULL,
    ALTER COLUMN legacy_stock_quantity DROP DEFAULT,
    ALTER COLUMN status SET DEFAULT 'INACTIVE';
CREATE FUNCTION app.protect_legacy_catalog_archive() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.legacy_sku IS NOT NULL OR NEW.legacy_price IS NOT NULL
            OR NEW.legacy_stock_quantity IS NOT NULL OR NEW.legacy_category IS NOT NULL
            OR NEW.legacy_brand IS NOT NULL THEN
            RAISE EXCEPTION 'Legacy catalog archive is read-only' USING ERRCODE = '23514';
        END IF;
    ELSIF ROW(NEW.legacy_sku,NEW.legacy_price,NEW.legacy_stock_quantity,NEW.legacy_category,NEW.legacy_brand)
        IS DISTINCT FROM ROW(OLD.legacy_sku,OLD.legacy_price,OLD.legacy_stock_quantity,OLD.legacy_category,OLD.legacy_brand) THEN
        RAISE EXCEPTION 'Legacy catalog archive is read-only' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_legacy_catalog_archive BEFORE INSERT OR UPDATE ON app.product
    FOR EACH ROW EXECUTE FUNCTION app.protect_legacy_catalog_archive();
COMMENT ON COLUMN app.product.legacy_price IS 'V2 archive only; live selling price is app.hm_sku.selling_price';
COMMENT ON COLUMN app.product.legacy_stock_quantity IS 'V2 archive only; live stock is app.hm_inventory';
CREATE INDEX ix_catalog_sku_product ON app.hm_sku(product_id, id)
    WHERE status='ACTIVE' AND NOT is_deleted;
