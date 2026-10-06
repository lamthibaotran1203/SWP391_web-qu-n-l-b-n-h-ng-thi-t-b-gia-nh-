package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.api.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;
    public CatalogRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String FROM = """
        from app.hm_sku s join app.product p on p.product_id=s.product_id
        join app.hm_category c on c.id=p.category_id
        join app.hm_brand b on b.id=p.brand_id
        left join app.hm_inventory i on i.sku_id=s.id
        where p.status='ACTIVE' and not p.is_deleted
          and s.status='ACTIVE' and not s.is_deleted
          and not c.is_deleted and not b.is_deleted
        """;
    private static final String SELECT = """
        select s.id sku_id,p.product_id,p.name product_name,s.sku_code,s.sku_name,
          coalesce(nullif(btrim(s.image_url),''),p.image_url) image_url,
          c.id category_id,c.name category_name,b.id brand_id,b.name brand_name,
          s.selling_price,coalesce(i.available_quantity,0) available_quantity,
          p.version product_version,s.version sku_version
        """;
    private static final RowMapper<CatalogSku> MAPPER = (r,n) -> new CatalogSku(
        r.getLong("sku_id"),r.getLong("product_id"),r.getString("product_name"),
        r.getString("sku_code"),r.getString("sku_name"),r.getString("image_url"),
        r.getLong("category_id"),r.getString("category_name"),r.getLong("brand_id"),
        r.getString("brand_name"),r.getBigDecimal("selling_price").toPlainString(),r.getInt("available_quantity"),
        r.getInt("available_quantity")>0,r.getLong("product_version"),r.getLong("sku_version"));
    public List<CatalogSku> page(int page,int size) {
        return jdbc.query(SELECT+FROM+" order by s.id limit ? offset ?",MAPPER,size,(long)page*size);
    }
    public long count() { return jdbc.queryForObject("select count(*) "+FROM,Long.class); }
    public List<CatalogSku> bySku(long id) { return jdbc.query(SELECT+FROM+" and s.id=?",MAPPER,id); }
    public List<CatalogSku> byProduct(long id) {
        return jdbc.query(SELECT+FROM+" and p.product_id=? order by s.id",MAPPER,id);
    }
    public List<CatalogReference> categories() {
        return jdbc.query("select id,name from app.hm_category where not is_deleted order by id",
            (r,n)->new CatalogReference(r.getLong(1),r.getString(2)));
    }
    public List<CatalogReference> brands() {
        return jdbc.query("select id,name from app.hm_brand where not is_deleted order by id",
            (r,n)->new CatalogReference(r.getLong(1),r.getString(2)));
    }
}
