package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.cart.CartService.Item;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import com.swp391.householdgoods.catalog.infrastructure.PublicCategoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {
    public record Line(int quantity, UUID version) {}
    public record AddRequest(long skuId, int quantity) {}
    private final JdbcTemplate jdbc;

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> findActiveCustomerIds(String email, boolean lockForUpdate) {
        String sql = """
                select user_id from app.user_account
                where lower(email)=lower(?) and role='CUSTOMER' and status='ACTIVE'
                """;
        if (lockForUpdate) {
            sql += " for update";
        }
        return jdbc.queryForList(sql, Long.class, email);
    }

    public List<Item> findItems(long customerId) {
        String sql = """
                select s.id,s.product_id,p.name,s.sku_name,s.sku_code,c.version,
                  coalesce(nullif(s.image_url,''),p.image_url) image_url,c.quantity,s.selling_price,
                  coalesce(i.available_quantity,0) stock,
                  (p.status='ACTIVE' and not p.is_deleted and s.status='ACTIVE' and not s.is_deleted
                   and not b.is_deleted and not cat.is_deleted
                   and cat.id in %s) eligible
                from app.hm_cart c join app.hm_sku s on s.id=c.sku_id
                join app.product p on p.product_id=s.product_id
                left join app.hm_brand b on b.id=p.brand_id
                left join app.hm_category cat on cat.id=p.category_id
                left join app.hm_inventory i on i.sku_id=s.id
                where c.user_id=? order by c.id
                """.formatted(PublicCategoryRepository.IDS);
        return jdbc.query(sql, this::mapItem, customerId);
    }

    private Item mapItem(ResultSet row, int rowNumber) throws SQLException {
        int quantity = row.getInt("quantity");
        BigDecimal price = row.getBigDecimal("selling_price");
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
        int availableStock = row.getInt("stock");
        boolean eligible = row.getBoolean("eligible");
        boolean available = eligible && quantity <= availableStock;

        return new Item(row.getLong("id"), row.getLong("product_id"), row.getString("name"),
                row.getString("sku_name"), row.getString("sku_code"), row.getString("image_url"),
                quantity, price.toPlainString(), lineTotal.toPlainString(), availableStock,
                eligible, available, row.getObject("version", UUID.class));
    }

    public long findQuantity(long customerId, long skuId) {
        String sql = """
                select coalesce(sum(quantity),0) from app.hm_cart where user_id=? and sku_id=?
                """;
        return jdbc.queryForObject(sql, Long.class, customerId, skuId);
    }

    public List<Integer> findEligibleStock(long skuId) {
        String sql = """
                select coalesce(i.available_quantity,0) from app.hm_sku s
                join app.product p on p.product_id=s.product_id
                join app.hm_brand b on b.id=p.brand_id join app.hm_category c on c.id=p.category_id
                left join app.hm_inventory i on i.sku_id=s.id
                where s.id=? and s.status='ACTIVE' and not s.is_deleted
                and p.status='ACTIVE' and not p.is_deleted and not b.is_deleted and not c.is_deleted
                and c.id in %s
                """.formatted(PublicCategoryRepository.IDS);
        return jdbc.queryForList(sql, Integer.class, skuId);
    }

    public void saveQuantity(long customerId, long skuId, int quantity, String email) {
        // Một dòng cho mỗi SKU: tạo mới nếu chưa có, cập nhật nếu đã tồn tại.
        String sql = """
                insert into app.hm_cart(user_id,sku_id,quantity,created_by,created_at)
                values (?,?,?,?,current_timestamp)
                on conflict(user_id,sku_id) do update set quantity=excluded.quantity,
                  updated_by=excluded.created_by,updated_at=current_timestamp,version=gen_random_uuid()
                """;
        jdbc.update(sql, customerId, skuId, quantity, email);
    }

    public void deleteItem(long customerId, long skuId) {
        jdbc.update("delete from app.hm_cart where user_id=? and sku_id=?", customerId, skuId);
    }

    public List<Line> findLine(long customerId, long skuId) {
        return jdbc.query("select quantity,version from app.hm_cart where user_id=? and sku_id=?",
                (row, index) -> new Line(row.getInt("quantity"), row.getObject("version", UUID.class)),
                customerId, skuId);
    }

    public List<AddRequest> findAddRequest(long customerId, UUID requestId) {
        return jdbc.query("select sku_id,quantity from app.hm_cart_add_request where user_id=? and request_id=?",
                (row, index) -> new AddRequest(row.getLong("sku_id"), row.getInt("quantity")),
                customerId, requestId);
    }

    public void saveAddRequest(long customerId, UUID requestId, long skuId, int quantity) {
        jdbc.update("insert into app.hm_cart_add_request(user_id,request_id,sku_id,quantity) values (?,?,?,?)",
                customerId, requestId, skuId, quantity);
    }
}
