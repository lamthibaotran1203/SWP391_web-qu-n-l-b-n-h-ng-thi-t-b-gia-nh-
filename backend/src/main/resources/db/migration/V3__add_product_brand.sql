ALTER TABLE app.product ADD COLUMN brand VARCHAR(100);

CREATE INDEX ix_product_brand_lower ON app.product (lower(brand));
