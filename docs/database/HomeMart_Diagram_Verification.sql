-- Run after HomeMart_From_Diagram_PostgreSQL.sql on a disposable review DB.
-- Fixtures are rolled back. PASS GAP means the drawing does NOT enforce that rule.
BEGIN;
DO $$
DECLARE
    u bigint; category bigint; brand bigint; product bigint;
    sku1 bigint; sku2 bigint; ord bigint; ret bigint; n integer;
BEGIN
    SELECT count(*) INTO n FROM information_schema.tables
        WHERE table_schema = 'app' AND table_name LIKE 'hm\_%' ESCAPE '\';
    IF n <> 23 THEN RAISE EXCEPTION 'Expected 23 hm tables, got %', n; END IF;
    SELECT count(*) INTO n FROM information_schema.columns
        WHERE table_schema = 'app' AND table_name LIKE 'hm\_%' ESCAPE '\';
    IF n <> 277 THEN RAISE EXCEPTION 'Expected 277 columns, got %', n; END IF;
    RAISE NOTICE 'PASS: 23 tables / 277 columns';

    INSERT INTO app.hm_user(account,email,password,full_name,created_date)
        VALUES ('diagram-review','diagram-review@example.invalid','HASH_FIXTURE_ONLY','Review',CURRENT_TIMESTAMP)
        RETURNING id INTO u;
    INSERT INTO app.hm_category(code,name,created_date)
        VALUES ('REVIEW-CAT','Review category',CURRENT_TIMESTAMP) RETURNING id INTO category;
    INSERT INTO app.hm_brand(name,created_date)
        VALUES ('Review brand',CURRENT_TIMESTAMP) RETURNING id INTO brand;
    INSERT INTO app.hm_product(category_id,brand_id,code,name,status,created_date)
        VALUES (category,brand,'REVIEW-P','Review product','ACTIVE',CURRENT_TIMESTAMP)
        RETURNING id INTO product;
    INSERT INTO app.hm_sku(product_id,sku_code,sku_name,original_price,selling_price,status,created_date)
        VALUES (product,'REVIEW-S1','Purchased SKU',100000,100000,'ACTIVE',CURRENT_TIMESTAMP)
        RETURNING id INTO sku1;
    INSERT INTO app.hm_sku(product_id,sku_code,sku_name,original_price,selling_price,status,created_date)
        VALUES (product,'REVIEW-S2','Not purchased SKU',100000,100000,'ACTIVE',CURRENT_TIMESTAMP)
        RETURNING id INTO sku2;

    INSERT INTO app.hm_cart(user_id,sku_id,quantity,created_date)
        VALUES (u,sku1,1,CURRENT_TIMESTAMP);
    BEGIN
        INSERT INTO app.hm_cart(user_id,sku_id,quantity,created_date)
            VALUES (u,sku1,1,CURRENT_TIMESTAMP);
        RAISE EXCEPTION 'Duplicate cart line unexpectedly accepted';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'PASS: duplicate cart SKU rejected';
    END;
    BEGIN
        UPDATE app.hm_cart SET quantity = 0 WHERE user_id = u;
        RAISE EXCEPTION 'Zero quantity unexpectedly accepted';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: zero cart quantity rejected';
    END;
    BEGIN
        INSERT INTO app.hm_cart(user_id,sku_id,quantity,created_date)
            VALUES (u,-999999,1,CURRENT_TIMESTAMP);
        RAISE EXCEPTION 'Orphan SKU unexpectedly accepted';
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE 'PASS: orphan cart SKU rejected';
    END;

    INSERT INTO app.hm_inventory(sku_id,physical_quantity,reserved_quantity,created_date)
        VALUES (sku1,10,3,CURRENT_TIMESTAMP);
    SELECT available_quantity INTO n FROM app.hm_inventory WHERE sku_id = sku1;
    IF n <> 7 THEN RAISE EXCEPTION 'Incorrect available balance'; END IF;
    UPDATE app.hm_inventory SET reserved_quantity = 4 WHERE sku_id = sku1;
    SELECT available_quantity INTO n FROM app.hm_inventory WHERE sku_id = sku1;
    IF n <> 6 THEN RAISE EXCEPTION 'Generated balance failed to update'; END IF;
    RAISE NOTICE 'PASS: generated available stock follows reservations';
    BEGIN
        UPDATE app.hm_inventory SET reserved_quantity = 11 WHERE sku_id = sku1;
        RAISE EXCEPTION 'Over-reservation unexpectedly accepted';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: reserved > physical rejected';
    END;
    BEGIN
        UPDATE app.hm_inventory SET reserved_quantity = -1 WHERE sku_id = sku1;
        RAISE EXCEPTION 'Negative reservation unexpectedly accepted';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: negative reserved stock rejected';
    END;

    INSERT INTO app.hm_address(user_id,recipient_name,recipient_phone,province,district,ward,street_address,is_default,created_date)
        VALUES (u,'Review','0000000000','P','D','W','S',true,CURRENT_TIMESTAMP);
    BEGIN
        INSERT INTO app.hm_address(user_id,recipient_name,recipient_phone,province,district,ward,street_address,is_default,created_date)
            VALUES (u,'Review','0000000000','P','D','W','S2',true,CURRENT_TIMESTAMP);
        RAISE EXCEPTION 'Second default unexpectedly accepted';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'PASS: second default address rejected';
    END;
    UPDATE app.hm_address SET is_default = false WHERE user_id = u;
    RAISE NOTICE 'PASS GAP: saved addresses can have zero defaults (UC-04 service rule needed)';
    UPDATE app.hm_address SET is_default = true, is_deleted = true WHERE user_id = u;
    BEGIN
        INSERT INTO app.hm_address(user_id,recipient_name,recipient_phone,province,district,ward,street_address,is_default,created_date)
            VALUES (u,'Review','0000000000','P','D','W','S3',true,CURRENT_TIMESTAMP);
        RAISE EXCEPTION 'Soft-deleted default behavior differs from source conversion';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'PASS GAP: soft-deleted default still blocks replacement (policy needed)';
    END;
    INSERT INTO app.hm_category(code,name,created_date)
        VALUES ('REVIEW-CAT-2','Review category',CURRENT_TIMESTAMP);
    RAISE NOTICE 'PASS GAP: duplicate root category name accepted (UC-11)';
    UPDATE app.hm_category SET parent_id = id WHERE id = category;
    RAISE NOTICE 'PASS GAP: self-parent category accepted (UC-11)';

    INSERT INTO app.hm_order(user_id,order_code,order_type,status,payment_status,total_amount,final_amount,created_date)
        VALUES (u,'REVIEW-O','ONLINE','DELIVERED','PAID',100000,100000,CURRENT_TIMESTAMP)
        RETURNING id INTO ord;
    INSERT INTO app.hm_order_item(order_id,sku_id,quantity,unit_price,total_price,created_date)
        VALUES (ord,sku1,1,100000,100000,CURRENT_TIMESTAMP);
    INSERT INTO app.hm_return_request(order_id,return_code,request_type,reason,status,created_date)
        VALUES (ord,'REVIEW-R','ONLINE','Review','PENDING',CURRENT_TIMESTAMP)
        RETURNING id INTO ret;
    INSERT INTO app.hm_return_item(return_request_id,sku_id,quantity,condition_status,refund_amount,created_date)
        VALUES (ret,sku2,999,'UNINSPECTED',0,CURRENT_TIMESTAMP);
    RAISE NOTICE 'PASS GAP: return of unpurchased SKU and excessive quantity accepted (UC-32)';

    INSERT INTO app.hm_payment(order_id,transaction_code,idempotency_key,amount,payment_method,status,created_date)
        VALUES (ord,'REVIEW-T','REVIEW-IDEM',100000,'ONLINE','PENDING',CURRENT_TIMESTAMP);
    BEGIN
        INSERT INTO app.hm_payment(order_id,transaction_code,idempotency_key,amount,payment_method,status,created_date)
            VALUES (ord,'REVIEW-T-2','REVIEW-IDEM',100000,'ONLINE','PENDING',CURRENT_TIMESTAMP);
        RAISE EXCEPTION 'Duplicate payment idempotency unexpectedly accepted';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'PASS: duplicate payment idempotency key rejected';
    END;
    RAISE NOTICE 'Verification finished; all fixtures will roll back';
END $$;
ROLLBACK;
