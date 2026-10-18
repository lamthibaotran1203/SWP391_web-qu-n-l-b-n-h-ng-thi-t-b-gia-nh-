package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.api.PublicCategory;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PublicCategoryRepository {
    // Bắt đầu từ danh mục gốc ACTIVE, rồi lấy các con ACTIVE theo từng cấp.
    // Cách này cũng ẩn nhánh có cha bị ẩn, danh mục mồ côi hoặc cây bị vòng lặp.
    public static final String VISIBLE = """
            with recursive visible_categories as (
              select id,parent_id,name,description,image_url from app.hm_category
              where parent_id is null and status='ACTIVE' and not is_deleted
              union all
              select c.id,c.parent_id,c.name,c.description,c.image_url from app.hm_category c
              join visible_categories p on c.parent_id=p.id
              where c.status='ACTIVE' and not c.is_deleted
            )
            """;
    public static final String IDS = "(" + VISIBLE + " select id from visible_categories)";
    private final JdbcTemplate jdbc;
    public PublicCategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public List<PublicCategory> all() {
        return jdbc.query(VISIBLE + """
                select c.*,exists(select 1 from visible_categories child where child.parent_id=c.id) has_children
                from visible_categories c order by lower(c.name),c.id
                """, (r, n) -> new PublicCategory(r.getLong("id"), r.getObject("parent_id", Long.class),
                r.getString("name"), r.getString("description"), r.getString("image_url"), r.getBoolean("has_children")));
    }
    public boolean visible(long id) {
        return jdbc.queryForObject(VISIBLE + "select count(*) from visible_categories where id=?", Long.class, id) > 0;
    }
    public boolean brandExists(long id) {
        return jdbc.queryForObject("select count(*) from app.hm_brand where id=? and not is_deleted", Long.class, id) > 0;
    }
}
