package com.swp391.householdgoods.catalog.management.infrastructure;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.ProductFields;
import com.swp391.householdgoods.catalog.management.api.ManagedProduct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ManagedProductRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            select product_id,name,description,category_id,brand_id,image_url,status,version
            from app.product where not is_deleted
            """;

    public ManagedProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ManagedProduct> find(long id, boolean lock) {
        return jdbc.query(SELECT + " and product_id=?" + (lock ? " for update" : ""),
                this::map, id).stream().findFirst();
    }

    public List<ManagedProduct> page(String search, String status, int page, int size) {
        return jdbc.query(SELECT + " and (?='' or lower(name) like ?) and (?='' or status=?)"
                        + " order by product_id desc limit ? offset ?", this::map,
                search, "%" + search + "%", status, status, size, (long) page * size);
    }

    public long count(String search, String status) {
        return jdbc.queryForObject("select count(*) from app.product where not is_deleted"
                        + " and (?='' or lower(name) like ?) and (?='' or status=?)", Long.class,
                search, "%" + search + "%", status, status);
    }

    public boolean categoryExists(long id, boolean lock) {
        return !jdbc.queryForList("select id from app.hm_category where id=? and not is_deleted"
                + (lock ? " for share" : ""), Long.class, id).isEmpty();
    }

    public boolean brandExists(long id, boolean lock) {
        return !jdbc.queryForList("select id from app.hm_brand where id=? and not is_deleted"
                + (lock ? " for share" : ""), Long.class, id).isEmpty();
    }

    public long insert(ProductFields fields, Instant now) {
        return jdbc.queryForObject("""
                insert into app.product(name,description,category_id,brand_id,image_url,status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?) returning product_id
                """, Long.class, fields.name(), fields.description(), fields.categoryId(), fields.brandId(),
                fields.imageUrl(), fields.status().name(), Timestamp.from(now), Timestamp.from(now));
    }

    public int update(long id, long version, ProductFields fields, Instant now) {
        return jdbc.update("""
                update app.product set name=?,description=?,category_id=?,brand_id=?,image_url=?,status=?,
                  updated_at=?,version=version+1 where product_id=? and version=? and not is_deleted
                """, fields.name(), fields.description(), fields.categoryId(), fields.brandId(),
                fields.imageUrl(), fields.status().name(), Timestamp.from(now), id, version);
    }

    public void touch(long id, Instant now) {
        jdbc.update("update app.product set updated_at=?,version=version+1 where product_id=?",
                Timestamp.from(now), id);
    }

    private ManagedProduct map(ResultSet row, int number) throws SQLException {
        return new ManagedProduct(row.getLong("product_id"), row.getString("name"), row.getString("description"),
                row.getObject("category_id", Long.class), row.getObject("brand_id", Long.class),
                row.getString("image_url"), ProductStatus.valueOf(row.getString("status")),
                row.getLong("version"), List.of());
    }
}
