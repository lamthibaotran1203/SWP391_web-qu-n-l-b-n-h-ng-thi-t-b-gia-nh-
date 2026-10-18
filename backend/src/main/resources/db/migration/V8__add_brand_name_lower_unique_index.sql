-- Ensure case-insensitive uniqueness for brand name at database level without breaking existing schema
CREATE UNIQUE INDEX IF NOT EXISTS ux_hm_brand_name_lower ON app.hm_brand (lower(btrim(name)));
