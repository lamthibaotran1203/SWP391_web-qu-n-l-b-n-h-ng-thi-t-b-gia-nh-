package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.application.ProductNotFoundException;
import com.swp391.householdgoods.catalog.application.dto.CreateProductRequest;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** All public product endpoint IDs identify sale units (hm_sku.id). */
@Repository
public class ProductWriteRepository {
    private final JdbcTemplate jdbc;

    public ProductWriteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Long referenceId(String table, String name) {
        if (name == null) return null;
        if (!table.equals("hm_category") && !table.equals("hm_brand")) throw new IllegalArgumentException("Invalid reference table");
        var ids = jdbc.queryForList("select id from app." + table + " where lower(name)=lower(?) and not is_deleted for share", Long.class, name);
        if (ids.size() != 1) throw new IllegalArgumentException("Danh mục/thương hiệu phải tồn tại và chưa bị xóa.");
        return ids.getFirst();
    }

    public void guardSku(String code, Long exceptSkuId) {
        // Serialize equal normalized codes, including concurrent requests before INSERT.
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(lower(?),0))", code);
        boolean exists = exceptSkuId == null
                ? jdbc.queryForObject("select exists(select 1 from app.hm_sku where lower(sku_code)=lower(?))", Boolean.class, code)
                : jdbc.queryForObject("select exists(select 1 from app.hm_sku where lower(sku_code)=lower(?) and id<>?)", Boolean.class, code, exceptSkuId);
        if (exists) throw new org.springframework.dao.DuplicateKeyException("SKU duplicate");
    }

    public ProductResponse insert(CreateProductRequest request, Long categoryId, Long brandId, ProductStatus status) {
        long productId = jdbc.queryForObject("""
                insert into app.product(name,category_id,brand_id,description,image_url,status,created_at,updated_at)
                values (?,?,?,?,?,?,now(),now()) returning product_id
                """, Long.class, request.name().trim(), categoryId, brandId, request.description(), request.imageUrl(), status.name());
        long skuId = jdbc.queryForObject("""
                insert into app.hm_sku(product_id,sku_code,barcode,sku_name,image_url,original_price,selling_price,status,created_at)
                values (?,?,?,?,?,?,?,?,now()) returning id
                """, Long.class, productId, request.sku().trim(), request.sku().trim(), request.name().trim(), request.imageUrl(), request.price(), request.price(), status.name());
        // Stock belongs to stock UCs. Missing inventory is correctly projected as zero.
        return find(skuId);
    }

    public ProductResponse lock(long skuId) {
        var products = jdbc.queryForList("select product_id from app.hm_sku where id=? and not is_deleted", Long.class, skuId);
        if (products.isEmpty()) throw new ProductNotFoundException(skuId);
        jdbc.queryForList("select product_id from app.product where product_id=? and not is_deleted for update", products.getFirst());
        jdbc.queryForList("select id from app.hm_sku where id=? for update", skuId);
        return find(skuId);
    }

    public ProductResponse update(ProductResponse current, CreateProductRequest request, Long categoryId, Long brandId, ProductStatus status) {
        jdbc.update("""
                update app.product set name=?,category_id=?,brand_id=?,description=?,image_url=?,status=?,updated_at=now(),version=version+1
                where product_id=?
                """, request.name().trim(), categoryId, brandId, request.description(), request.imageUrl(), status.name(), current.productId());
        jdbc.update("""
                update app.hm_sku set sku_code=?,sku_name=?,image_url=?,selling_price=?,status=?,updated_at=now(),version=version+1 where id=?
                """, request.sku().trim(), request.name().trim(), request.imageUrl(), request.price(), status.name(), current.id());
        // Preserve barcode, original price, sibling SKUs and every inventory/ledger row.
        return find(current.id());
    }

    public void softDelete(long skuId) {
        jdbc.update("update app.hm_sku set status='INACTIVE',is_deleted=true,updated_at=now(),version=version+1 where id=?", skuId);
    }

    public ProductResponse find(long skuId) {
        var rows = jdbc.query("""
                select s.id,p.product_id,s.sku_code,p.name,b.name brand,c.name category,s.selling_price,
                    coalesce(i.available_quantity,0) available,p.description,coalesce(nullif(btrim(s.image_url),''),p.image_url) image,
                    s.status,p.created_at,p.updated_at,p.version product_version,s.version sku_version
                from app.hm_sku s join app.product p on p.product_id=s.product_id
                left join app.hm_brand b on b.id=p.brand_id left join app.hm_category c on c.id=p.category_id
                left join app.hm_inventory i on i.sku_id=s.id
                where s.id=? and not s.is_deleted and not p.is_deleted
                """, (row, index) -> new ProductResponse(row.getLong("id"),row.getString("sku_code"),row.getString("name"),
                row.getString("brand"),row.getString("category"),row.getBigDecimal("selling_price"),row.getInt("available"),
                row.getString("description"),row.getString("image"),ProductStatus.valueOf(row.getString("status")),
                row.getTimestamp("created_at").toInstant(),row.getTimestamp("updated_at").toInstant(),row.getLong("product_id"),
                row.getLong("product_version"),row.getLong("sku_version")),skuId);
        if (rows.isEmpty()) throw new ProductNotFoundException(skuId);
        return rows.getFirst();
    }
}
