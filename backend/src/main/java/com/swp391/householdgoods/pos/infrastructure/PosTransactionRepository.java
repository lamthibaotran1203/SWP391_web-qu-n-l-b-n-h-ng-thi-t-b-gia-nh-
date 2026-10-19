package com.swp391.householdgoods.pos.infrastructure;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.swp391.householdgoods.catalog.infrastructure.PublicCategoryRepository;
import com.swp391.householdgoods.pos.api.PosPaymentMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PosTransactionRepository {
    public record SaleSku(
            long skuId,
            String productName,
            String skuName,
            String skuCode,
            BigDecimal unitPrice,
            int availableQuantity) {}

    public record SaleLine(long skuId, int quantity) {}

    public record ExistingTransaction(long orderId, String paymentMethod, List<SaleLine> items) {}

    private final JdbcTemplate jdbc;

    public PosTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> findActiveStaffId(String email) {
        List<Long> rows = jdbc.queryForList(
                """
                select user_id from app.user_account
                where lower(email)=lower(?) and role in ('STAFF','ADMIN') and status='ACTIVE'
                """,
                Long.class,
                email);
        return rows.stream().findFirst();
    }

    public Optional<ExistingTransaction> findBySubmissionKey(UUID submissionKey) {
        List<Long> orderIds = jdbc.queryForList(
                """
                select id from app.hm_order
                where submission_key=? and order_type='COUNTER'
                  and status='COMPLETED' and payment_status='PAID'
                """,
                Long.class,
                submissionKey.toString());
        if (orderIds.isEmpty()) {
            return Optional.empty();
        }

        long orderId = orderIds.getFirst();
        String paymentMethod = jdbc.queryForObject(
                """
                select payment_method from app.hm_payment
                where order_id=? and status='COMPLETED'
                order by created_at desc, id desc limit 1
                """,
                String.class,
                orderId);
        List<SaleLine> items = jdbc.query(
                "select sku_id, quantity from app.hm_order_item where order_id=? order by sku_id",
                (row, index) -> new SaleLine(row.getLong("sku_id"), row.getInt("quantity")),
                orderId);
        return Optional.of(new ExistingTransaction(orderId, paymentMethod, items));
    }

    public List<SaleSku> lockSellableSkus(List<Long> skuIds) {
        String placeholders = String.join(",", java.util.Collections.nCopies(skuIds.size(), "?"));
        String sql = """
                select s.id sku_id, p.name product_name, s.sku_name, s.sku_code,
                       s.selling_price, i.available_quantity
                from app.hm_sku s
                join app.product p on p.product_id=s.product_id
                join app.hm_brand b on b.id=p.brand_id
                join app.hm_category c on c.id=p.category_id
                join app.hm_inventory i on i.sku_id=s.id
                where p.status='ACTIVE' and not p.is_deleted
                  and s.status='ACTIVE' and not s.is_deleted
                  and not b.is_deleted and not c.is_deleted
                  and c.id in %s
                  and s.id in (%s)
                order by s.id
                for update of s, i
                """.formatted(PublicCategoryRepository.IDS, placeholders);
        return jdbc.query(sql, this::mapSku, skuIds.toArray());
    }

    public Optional<Long> createOrder(
            UUID submissionKey,
            String orderCode,
            BigDecimal total,
            String actorEmail) {
        List<Long> rows = jdbc.query(
                """
                insert into app.hm_order (
                    order_code, order_type, status, payment_status, total_amount,
                    discount_amount, shipping_fee, final_amount, created_by, created_at,
                    updated_by, updated_at, submission_key
                ) values (?, 'COUNTER', 'COMPLETED', 'PAID', ?, 0, 0, ?, ?,
                    current_timestamp, ?, current_timestamp, ?)
                on conflict (submission_key) do nothing
                returning id
                """,
                (row, index) -> row.getLong("id"),
                orderCode,
                total,
                total,
                actorEmail,
                actorEmail,
                submissionKey.toString());
        return rows.stream().findFirst();
    }

    public boolean deductStock(
            long orderId,
            UUID submissionKey,
            long skuId,
            int quantity,
            String actorEmail) {
        List<Integer> balances = jdbc.query(
                """
                update app.hm_inventory
                set physical_quantity=physical_quantity-?, version=version+1,
                    updated_by=?, updated_at=current_timestamp
                where sku_id=? and physical_quantity-reserved_quantity>=?
                returning physical_quantity
                """,
                (row, index) -> row.getInt("physical_quantity"),
                quantity,
                actorEmail,
                skuId,
                quantity);
        if (balances.isEmpty()) {
            return false;
        }
        jdbc.update(
                """
                insert into app.hm_stock_movement (
                    sku_id, movement_type, quantity_change, balance_after,
                    reference_type, reference_id, movement_date, created_by, created_at, posting_key
                ) values (?, 'SALE', ?, ?, 'ORDER', ?, current_timestamp, ?, current_timestamp, ?)
                """,
                skuId,
                -quantity,
                balances.getFirst(),
                orderId,
                actorEmail,
                "POS:" + submissionKey + ":SKU:" + skuId);
        return true;
    }

    public void addOrderItem(long orderId, SaleSku sku, int quantity, String actorEmail) {
        jdbc.update(
                """
                insert into app.hm_order_item (
                    order_id, sku_id, quantity, unit_price, total_price,
                    created_by, created_at, updated_by, updated_at,
                    sku_code_snapshot, sku_name_snapshot, discount_amount
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp, ?, ?, 0)
                """,
                orderId,
                sku.skuId(),
                quantity,
                sku.unitPrice(),
                sku.unitPrice().multiply(BigDecimal.valueOf(quantity)),
                actorEmail,
                actorEmail,
                sku.skuCode(),
                sku.productName() + " - " + sku.skuName());
    }

    public void addPayment(
            long orderId,
            UUID submissionKey,
            BigDecimal total,
            PosPaymentMethod paymentMethod,
            String actorEmail) {
        jdbc.update(
                """
                insert into app.hm_payment (
                    order_id, transaction_code, idempotency_key, amount, payment_method,
                    status, payment_time, created_by, created_at, updated_by, updated_at,
                    provider
                ) values (?, ?, ?, ?, ?, 'COMPLETED', current_timestamp, ?,
                    current_timestamp, ?, current_timestamp, 'IN_STORE')
                """,
                orderId,
                "POS-" + UUID.randomUUID(),
                submissionKey.toString(),
                total,
                paymentMethod.name(),
                actorEmail,
                actorEmail);
    }

    public void addCompletedEvent(long orderId, long actorId, UUID submissionKey) {
        jdbc.update(
                """
                insert into app.hm_order_status_event (
                    order_id, from_status, to_status, actor_user_id, occurred_at, reason, event_key
                ) values (?, null, 'COMPLETED', ?, current_timestamp, 'POS sale completed',
                    ?)
                """,
                orderId,
                actorId,
                "POS:" + submissionKey);
    }

    private SaleSku mapSku(ResultSet row, int rowNumber) throws SQLException {
        return new SaleSku(
                row.getLong("sku_id"),
                row.getString("product_name"),
                row.getString("sku_name"),
                row.getString("sku_code"),
                row.getBigDecimal("selling_price"),
                row.getInt("available_quantity"));
    }
}
