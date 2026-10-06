package com.swp391.householdgoods.catalog.management.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.SkuFields;
import com.swp391.householdgoods.catalog.management.api.ManagedSku;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ManagedSkuRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private static final String SELECT = """
            select s.*,coalesce(i.available_quantity,0) available_quantity
            from app.hm_sku s left join app.hm_inventory i on i.sku_id=s.id
            where s.product_id=? and not s.is_deleted
            """;

    public ManagedSkuRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<ManagedSku> byProduct(long productId) {
        return jdbc.query(SELECT + " order by s.id", (row, number) -> new ManagedSku(
                row.getLong("id"), row.getLong("product_id"), row.getString("sku_code"),
                row.getString("barcode"), row.getString("sku_name"), readAttributes(row.getString("attributes_json")),
                row.getString("image_url"), row.getBigDecimal("original_price").toPlainString(),
                row.getBigDecimal("selling_price").toPlainString(), row.getInt("min_stock_threshold"),
                ProductStatus.valueOf(row.getString("status")), row.getLong("version"),
                row.getInt("available_quantity"), row.getInt("available_quantity") < row.getInt("min_stock_threshold")),
                productId);
    }

    public Optional<Long> lockVersion(long productId, long skuId) {
        return jdbc.queryForList("select version from app.hm_sku where product_id=? and id=?"
                + " and not is_deleted for update", Long.class, productId, skuId).stream().findFirst();
    }

    public boolean codeExists(String code, long exceptId) {
        return jdbc.queryForObject("select exists(select 1 from app.hm_sku where sku_code=? and id<>?)",
                Boolean.class, code, exceptId);
    }

    public boolean barcodeExists(String barcode, long exceptId) {
        return jdbc.queryForObject("select exists(select 1 from app.hm_sku where barcode=? and id<>?)",
                Boolean.class, barcode, exceptId);
    }

    public long insert(long productId, SkuFields fields, String actor, Instant now) {
        return jdbc.queryForObject("""
                insert into app.hm_sku(product_id,sku_code,barcode,sku_name,attributes_json,image_url,
                  original_price,selling_price,min_stock_threshold,status,created_by,created_at)
                values (?,?,?,?,?::jsonb,?,?,?,?,?,?,?) returning id
                """, Long.class, productId, fields.skuCode(), fields.barcode(), fields.skuName(),
                writeAttributes(fields.attributes()), fields.imageUrl(), fields.originalPrice(), fields.sellingPrice(),
                fields.minStockThreshold(), fields.status().name(), actor, Timestamp.from(now));
    }

    public int update(long productId, long skuId, long version, SkuFields fields, String actor, Instant now) {
        return jdbc.update("""
                update app.hm_sku set sku_code=?,barcode=?,sku_name=?,attributes_json=?::jsonb,image_url=?,
                  original_price=?,selling_price=?,min_stock_threshold=?,status=?,updated_by=?,updated_at=?,
                  version=version+1 where product_id=? and id=? and version=? and not is_deleted
                """, fields.skuCode(), fields.barcode(), fields.skuName(), writeAttributes(fields.attributes()),
                fields.imageUrl(), fields.originalPrice(), fields.sellingPrice(), fields.minStockThreshold(),
                fields.status().name(), actor, Timestamp.from(now), productId, skuId, version);
    }

    private String writeAttributes(Map<String, String> attributes) {
        try {
            return json.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("Thuộc tính SKU chưa hợp lệ.");
        }
    }

    private Map<String, String> readAttributes(String value) {
        if (value == null) return Map.of();
        try {
            return json.readValue(value, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Stored SKU attributes must be an object");
        }
    }
}
