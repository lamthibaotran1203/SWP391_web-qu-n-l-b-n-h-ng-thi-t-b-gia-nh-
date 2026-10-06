-- Explicit opt-in fixture. Never run automatically by Flyway/application startup.
SELECT pg_advisory_xact_lock(39113005);
DO $$
DECLARE c bigint; b bigint; p bigint; s bigint; n integer; demo_code text;
BEGIN
  INSERT INTO app.hm_category(code,name,created_by,created_at)
  VALUES ('DEMO-CATALOG','Đồ gia dụng mẫu','DEMO_SEED',CURRENT_TIMESTAMP)
  ON CONFLICT(code) DO NOTHING;
  SELECT id INTO c FROM app.hm_category WHERE code='DEMO-CATALOG' AND NOT is_deleted;
  INSERT INTO app.hm_brand(name,created_by,created_at)
  VALUES ('HomeMart Demo','DEMO_SEED',CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
  SELECT id INTO b FROM app.hm_brand WHERE name='HomeMart Demo' AND NOT is_deleted;
  IF c IS NULL OR b IS NULL THEN RAISE EXCEPTION 'Demo references are inactive'; END IF;
  FOR n IN 1..2 LOOP
    demo_code := 'DEMO-SKU-' || n;
    IF NOT EXISTS (SELECT 1 FROM app.hm_sku WHERE sku_code=demo_code) THEN
      INSERT INTO app.product(name,description,category_id,brand_id,image_url,status,created_at,updated_at)
      VALUES (CASE WHEN n=1 THEN 'Nồi inox mẫu 2 lít' ELSE 'Nồi inox mẫu 3 lít' END,
        'Dữ liệu mẫu phục vụ tích hợp; không phải hàng hóa thực tế.',c,b,
        '/images/catalog-demo.svg','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING product_id INTO p;
      INSERT INTO app.hm_sku(product_id,sku_code,barcode,sku_name,original_price,selling_price,status,created_by,created_at)
      VALUES(p,demo_code,demo_code,CASE WHEN n=1 THEN 'Dung tích 2 lít' ELSE 'Dung tích 3 lít' END,
        250000,CASE WHEN n=1 THEN 200000 ELSE 230000 END,'ACTIVE','DEMO_SEED',CURRENT_TIMESTAMP) RETURNING id INTO s;
      INSERT INTO app.hm_inventory(sku_id,physical_quantity,reserved_quantity,created_by,created_at)
      VALUES(s,CASE WHEN n=1 THEN 5 ELSE 0 END,0,'DEMO_SEED',CURRENT_TIMESTAMP);
      IF n=1 THEN
        INSERT INTO app.hm_stock_movement(sku_id,movement_type,quantity_change,balance_after,
          reference_type,reference_id,movement_date,created_by,created_at,posting_key)
        VALUES(s,'OPENING_BALANCE',5,5,'DEMO_SEED',p,CURRENT_TIMESTAMP,'DEMO_SEED',CURRENT_TIMESTAMP,'DEMO_OPENING_'||demo_code);
      END IF;
    END IF;
  END LOOP;
END $$;

-- Real product photos from the manufacturer's pages. Prices/stock below are demo values.
-- https://sunhouse.com.vn/dien-gia-dung/noi-com-dien/sunhouse-shd8602.html
-- https://sunhouse.com.vn/dien-gia-dung/am-sieu-toc/sunhouse-shd1182.html
-- https://sunhouse.com.vn/do-gia-dung/bo-noi-nhom/bo-noi-nhom-chong-dinh-day-tu-sunhouse-eco-shg1122mx.html
DO $$
DECLARE item record; c bigint; b bigint; p bigint; s bigint;
BEGIN
  PERFORM pg_advisory_xact_lock(39113005);
  INSERT INTO app.hm_brand(name,created_by,created_at)
  VALUES ('SUNHOUSE','DEMO_SEED',CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
  SELECT id INTO b FROM app.hm_brand WHERE name='SUNHOUSE' AND NOT is_deleted;
  IF b IS NULL THEN RAISE EXCEPTION 'Demo brand is inactive'; END IF;
  FOR item IN SELECT * FROM (VALUES
    ('DEMO-SHD8602','Nồi cơm điện SUNHOUSE SHD8602 1.8 lít','DEMO-RICE-COOKER','Nồi cơm điện',
     'Nồi cơm điện dung tích 1.8 lít, lòng nồi chống dính.',
     'https://sunhouse.com.vn/pic/product/SHD8602.png',690000,590000,8,3,'{"Dung tích":"1.8 lít"}'),
    ('DEMO-SHD1182','Ấm siêu tốc SUNHOUSE SHD1182 1.8 lít','DEMO-KETTLE','Ấm siêu tốc',
     'Ấm đun nước thân inox, dung tích 1.8 lít.',
     'https://sunhouse.com.vn/pic/product/SHD1182.jpg',290000,249000,2,3,'{"Dung tích":"1.8 lít"}'),
    ('DEMO-SHG1122MX','Bộ nồi SUNHOUSE ECO SHG1122MX','DEMO-COOKWARE','Nồi và chảo',
     'Bộ nồi nhôm chống dính đáy từ SUNHOUSE ECO.',
     'https://sunhouse.com.vn/pic/product/ava(73).jpg',1290000,1090000,0,3,'{"Quy cách":"Bộ nồi"}')
  ) AS fixture(code,name,category_code,category_name,description,image,original_price,selling_price,quantity,threshold,attributes)
  LOOP
    INSERT INTO app.hm_category(code,name,created_by,created_at)
    VALUES(item.category_code,item.category_name,'DEMO_SEED',CURRENT_TIMESTAMP) ON CONFLICT(code) DO NOTHING;
    SELECT id INTO c FROM app.hm_category WHERE code=item.category_code AND NOT is_deleted;
    IF c IS NULL THEN RAISE EXCEPTION 'Demo category is inactive'; END IF;
    IF NOT EXISTS (SELECT 1 FROM app.hm_sku WHERE sku_code=item.code) THEN
      INSERT INTO app.product(name,description,category_id,brand_id,image_url,status,created_at,updated_at)
      VALUES(item.name,item.description,c,b,item.image,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING product_id INTO p;
      INSERT INTO app.hm_sku(product_id,sku_code,barcode,sku_name,attributes_json,image_url,
        original_price,selling_price,min_stock_threshold,status,created_by,created_at)
      VALUES(p,item.code,item.code,item.name,item.attributes::jsonb,item.image,item.original_price,
        item.selling_price,item.threshold,'ACTIVE','DEMO_SEED',CURRENT_TIMESTAMP) RETURNING id INTO s;
      INSERT INTO app.hm_inventory(sku_id,physical_quantity,reserved_quantity,created_by,created_at)
      VALUES(s,item.quantity,0,'DEMO_SEED',CURRENT_TIMESTAMP);
      IF item.quantity > 0 THEN
        INSERT INTO app.hm_stock_movement(sku_id,movement_type,quantity_change,balance_after,
          reference_type,reference_id,movement_date,created_by,created_at,posting_key)
        VALUES(s,'OPENING_BALANCE',item.quantity,item.quantity,'DEMO_SEED',p,
          CURRENT_TIMESTAMP,'DEMO_SEED',CURRENT_TIMESTAMP,'DEMO_OPENING_'||item.code);
      END IF;
    END IF;
  END LOOP;
END $$;
