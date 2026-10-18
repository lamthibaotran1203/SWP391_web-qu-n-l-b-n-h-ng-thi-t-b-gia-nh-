package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.api.CatalogueProduct;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService.Search;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogueBrowseRepository {
    private final JdbcTemplate jdbc;
    public CatalogueBrowseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    private Sql grouped(Search search) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(PublicCategoryRepository.VISIBLE + """
                select p.product_id,p.name,p.image_url,c.id category_id,c.name category_name,
                  b.id brand_id,b.name brand_name,min(s.selling_price) min_price,max(s.selling_price) max_price,
                  sum(coalesce(i.available_quantity,0)) available_quantity,count(*) sku_count
                from app.product p join app.hm_sku s on s.product_id=p.product_id
                join visible_categories c on c.id=p.category_id
                join app.hm_brand b on b.id=p.brand_id and not b.is_deleted
                left join app.hm_inventory i on i.sku_id=s.id
                where p.status='ACTIVE' and not p.is_deleted and s.status='ACTIVE' and not s.is_deleted
                """);
        if (search.search() != null && !search.search().isBlank()) {
            sql.append(" and (lower(p.name) like ? escape '!' or lower(s.sku_name) like ? escape '!' or lower(s.sku_code) like ? escape '!' or lower(b.name) like ? escape '!')");
            String pattern = keywordPattern(search.search());
            // Bốn dấu ? tương ứng tên Product, tên SKU, mã SKU và thương hiệu.
            for (int index = 0; index < 4; index++) {
                params.add(pattern);
            }
        }
        if (search.brandId() != null) {
            sql.append(" and b.id=?");
            params.add(search.brandId());
        }
        if (search.categoryId() != null) {
            sql.append(" and c.id in (with recursive subtree as (select id from visible_categories where id=? union all select child.id from visible_categories child join subtree parent on child.parent_id=parent.id) select id from subtree)");
            params.add(search.categoryId());
        }
        if (search.minPrice() != null) {
            sql.append(" and s.selling_price>=?");
            params.add(search.minPrice());
        }
        if (search.maxPrice() != null) {
            sql.append(" and s.selling_price<=?");
            params.add(search.maxPrice());
        }
        sql.append(" group by p.product_id,p.name,p.image_url,c.id,c.name,b.id,b.name");
        if (search.availability().equals("in_stock")) {
            sql.append(" having sum(coalesce(i.available_quantity,0))>0");
        }
        if (search.availability().equals("out_of_stock")) {
            sql.append(" having sum(coalesce(i.available_quantity,0))=0");
        }
        return new Sql(sql.toString(), params);
    }
    public long count(Search search) {
        Sql sql = grouped(search);
        return jdbc.queryForObject("select count(*) from (" + sql.text() + ") results", Long.class, sql.params().toArray());
    }
    public List<CatalogueProduct> page(Search search, int page) {
        Sql sql = grouped(search);
        List<Object> params = new ArrayList<>(sql.params());
        params.add(search.size());
        params.add((long) page * search.size());
        String order = orderBy(search.sort());
        String pagedSql = "select * from (" + sql.text() + ") results order by " + order + " limit ? offset ?";
        return jdbc.query(pagedSql, this::mapProduct, params.toArray());
    }

    private String keywordPattern(String keyword) {
        // % và _ do người dùng nhập là ký tự thật, không phải wildcard SQL.
        String escaped = keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private CatalogueProduct mapProduct(ResultSet row, int rowNumber) throws SQLException {
        return new CatalogueProduct(row.getLong("product_id"), row.getString("name"), row.getString("image_url"),
                row.getLong("category_id"), row.getString("category_name"), row.getLong("brand_id"), row.getString("brand_name"),
                row.getBigDecimal("min_price").toPlainString(), row.getBigDecimal("max_price").toPlainString(),
                row.getLong("available_quantity"), row.getInt("sku_count"));
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "oldest" -> "product_id";
            case "name_asc" -> "lower(name),product_id";
            case "name_desc" -> "lower(name) desc,product_id desc";
            case "price_asc" -> "min_price,product_id";
            case "price_desc" -> "min_price desc,product_id desc";
            default -> "product_id desc";
        };
    }
    private record Sql(String text, List<Object> params) {}
}
