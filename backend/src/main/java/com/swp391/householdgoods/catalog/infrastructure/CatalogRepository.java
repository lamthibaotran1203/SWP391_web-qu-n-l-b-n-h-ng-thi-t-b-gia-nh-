package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.api.CatalogReference;
import com.swp391.householdgoods.catalog.api.CatalogProductDetail;
import com.swp391.householdgoods.catalog.api.CatalogSku;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;

    public CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FROM = """
            from app.hm_sku s join app.product p on p.product_id=s.product_id
            join app.hm_category c on c.id=p.category_id
            join app.hm_brand b on b.id=p.brand_id
            left join app.hm_inventory i on i.sku_id=s.id
            where p.status='ACTIVE' and not p.is_deleted
              and s.status='ACTIVE' and not s.is_deleted
              and not c.is_deleted and not b.is_deleted
            """ + " and c.id in " + PublicCategoryRepository.IDS;
    private static final String SELECT = """
            select s.id sku_id,p.product_id,p.name product_name,s.sku_code,s.sku_name,
              coalesce(nullif(btrim(s.image_url),''),p.image_url) image_url,
              c.id category_id,c.name category_name,b.id brand_id,b.name brand_name,
              s.selling_price,coalesce(i.available_quantity,0) available_quantity,
              p.version product_version,s.version sku_version
            """;
    private static final RowMapper<CatalogSku> MAPPER = (row, number) -> new CatalogSku(
            row.getLong("sku_id"), row.getLong("product_id"), row.getString("product_name"),
            row.getString("sku_code"), row.getString("sku_name"), row.getString("image_url"),
            row.getLong("category_id"), row.getString("category_name"), row.getLong("brand_id"),
            row.getString("brand_name"), row.getBigDecimal("selling_price").toPlainString(),
            row.getInt("available_quantity"), row.getInt("available_quantity") > 0,
            row.getLong("product_version"), row.getLong("sku_version"));

    public List<CatalogSku> page(String search, String brand, String category,
            BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        FilterSql filter = filter(search, brand, category, minPrice, maxPrice);
        List<Object> parameters = new ArrayList<>(filter.parameters());
        parameters.add(size);
        parameters.add((long) page * size);
        return jdbc.query(SELECT + FROM + filter.sql() + orderBy(sort) + " limit ? offset ?",
                MAPPER, parameters.toArray());
    }

    public long count(String search, String brand, String category,
            BigDecimal minPrice, BigDecimal maxPrice) {
        FilterSql filter = filter(search, brand, category, minPrice, maxPrice);
        return jdbc.queryForObject("select count(*) " + FROM + filter.sql(),
                Long.class, filter.parameters().toArray());
    }

    public List<CatalogSku> bySku(long id) {
        return jdbc.query(SELECT + FROM + " and s.id=?", MAPPER, id);
    }

    public List<CatalogSku> byProduct(long id) {
        return jdbc.query(SELECT + FROM + " and p.product_id=? order by s.id", MAPPER, id);
    }

    public CatalogProductDetail productDetail(long id, List<CatalogSku> eligibleSkus) {
        CatalogSku first = eligibleSkus.getFirst();
        return jdbc.queryForObject("select description,image_url from app.product where product_id=?",
                (row, number) -> {
                    List<String> images = collectProductImages(row.getString("image_url"), eligibleSkus);
                    return new CatalogProductDetail(id, first.productName(), row.getString("description"),
                            first.categoryId(), first.categoryName(), first.brandId(), first.brandName(),
                            images, eligibleSkus);
                }, id);
    }

    private List<String> collectProductImages(String cover, List<CatalogSku> skus) {
        // Giữ ảnh bìa đứng trước, bỏ ảnh trống và không hiển thị ảnh trùng nhau.
        Set<String> images = new LinkedHashSet<>();
        if (cover != null && !cover.isBlank()) {
            images.add(cover.trim());
        }
        for (CatalogSku sku : skus) {
            String imageUrl = sku.imageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                images.add(imageUrl.trim());
            }
        }
        return List.copyOf(images);
    }

    public List<CatalogReference> categories() {
        return jdbc.query("select id,name from app.hm_category where not is_deleted order by id",
                (row, number) -> new CatalogReference(row.getLong(1), row.getString(2)));
    }

    public List<CatalogReference> brands() {
        return jdbc.query("select id,name from app.hm_brand where not is_deleted order by id",
                (row, number) -> new CatalogReference(row.getLong(1), row.getString(2)));
    }

    private FilterSql filter(String search, String brand, String category,
            BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        if (search != null) {
            sql.append(" and (lower(p.name) like ? or lower(s.sku_name) like ? "
                    + "or lower(s.sku_code) like ? or lower(b.name) like ?)");
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (brand != null) {
            sql.append(" and lower(b.name)=?");
            parameters.add(brand.toLowerCase(Locale.ROOT));
        }
        if (category != null) {
            sql.append(" and lower(c.name)=?");
            parameters.add(category.toLowerCase(Locale.ROOT));
        }
        if (minPrice != null) {
            sql.append(" and s.selling_price>=?");
            parameters.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" and s.selling_price<=?");
            parameters.add(maxPrice);
        }
        return new FilterSql(sql.toString(), parameters);
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "oldest" -> " order by s.id";
            case "name_asc" -> " order by lower(p.name),lower(s.sku_name),s.id";
            case "name_desc" -> " order by lower(p.name) desc,lower(s.sku_name) desc,s.id desc";
            case "price_asc" -> " order by s.selling_price,s.id";
            case "price_desc" -> " order by s.selling_price desc,s.id desc";
            default -> " order by s.id desc";
        };
    }

    private record FilterSql(String sql, List<Object> parameters) {}
}
