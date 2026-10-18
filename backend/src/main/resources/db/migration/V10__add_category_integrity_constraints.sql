ALTER TABLE app.hm_category
    ADD CONSTRAINT ck_hm_category_code_not_blank CHECK (btrim(code) <> ''),
    ADD CONSTRAINT ck_hm_category_name_not_blank CHECK (btrim(name) <> ''),
    ADD CONSTRAINT ck_hm_category_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id);

CREATE UNIQUE INDEX uq_hm_category_code_lower
    ON app.hm_category (lower(btrim(code)));

CREATE UNIQUE INDEX uq_hm_category_name_lower
    ON app.hm_category (lower(btrim(name)));
