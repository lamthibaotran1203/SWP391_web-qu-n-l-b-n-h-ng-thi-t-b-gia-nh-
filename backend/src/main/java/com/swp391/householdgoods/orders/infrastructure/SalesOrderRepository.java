package com.swp391.householdgoods.orders.infrastructure;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.api.SalesOrderLineDetails;
import com.swp391.householdgoods.orders.api.SalesOrderPage;
import com.swp391.householdgoods.orders.api.SalesOrderSummary;
import com.swp391.householdgoods.orders.application.SalesOrderSearch;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SalesOrderRepository {

    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ORDER_FROM = """
            from app.hm_order o
            left join app.user_account u on u.user_id = o.user_id
            left join lateral (
                select p.payment_method
                from app.hm_payment p
                where p.order_id = o.id
                order by p.created_at desc, p.id desc
                limit 1
            ) payment on true
            left join app.hm_shipment sh on sh.order_id = o.id
            """;
    private static final String ORDER_SELECT = """
            select o.id, o.order_code as order_number, o.order_type as channel,
                   o.status, o.payment_status, payment.payment_method,
                   coalesce(u.full_name, sh.recipient_name, 'Khách tại quầy') as customer_name,
                   u.email as customer_email, coalesce(u.phone, sh.recipient_phone) as customer_phone,
                   sh.shipping_address,
                   o.total_amount as subtotal, o.discount_amount, o.shipping_fee,
                   o.final_amount as total_amount, o.created_at as placed_at
            """;
    private static final RowMapper<SalesOrderSummary> SUMMARY_MAPPER = (row, rowNumber) ->
            new SalesOrderSummary(
                    row.getLong("id"),
                    row.getString("order_number"),
                    row.getString("channel"),
                    row.getString("status"),
                    row.getString("payment_status"),
                    row.getString("payment_method"),
                    row.getString("customer_name"),
                    row.getString("customer_email"),
                    row.getString("customer_phone"),
                    row.getBigDecimal("total_amount"),
                    row.getTimestamp("placed_at").toInstant());
    private static final RowMapper<SalesOrderDetails> DETAILS_MAPPER = (row, rowNumber) ->
            new SalesOrderDetails(
                    row.getLong("id"),
                    row.getString("order_number"),
                    row.getString("channel"),
                    row.getString("status"),
                    row.getString("payment_status"),
                    row.getString("payment_method"),
                    row.getString("customer_name"),
                    row.getString("customer_email"),
                    row.getString("customer_phone"),
                    row.getString("shipping_address"),
                    row.getBigDecimal("subtotal"),
                    row.getBigDecimal("discount_amount"),
                    row.getBigDecimal("shipping_fee"),
                    row.getBigDecimal("total_amount"),
                    row.getTimestamp("placed_at").toInstant(),
                    List.of());
    private static final RowMapper<SalesOrderLineDetails> LINE_MAPPER = (row, rowNumber) ->
            new SalesOrderLineDetails(
                    row.getLong("id"),
                    row.getString("product_name"),
                    row.getString("product_sku"),
                    row.getInt("quantity"),
                    row.getBigDecimal("unit_price"),
                    row.getBigDecimal("line_total"));

    private final JdbcTemplate jdbc;

    public SalesOrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SalesOrderPage<SalesOrderSummary> search(SalesOrderSearch search, int page, int size) {
        Filter filter = filter(search);
        List<Object> parameters = new ArrayList<>(filter.parameters());
        parameters.add(size);
        parameters.add((long) page * size);

        List<SalesOrderSummary> content = jdbc.query(
                ORDER_SELECT + ORDER_FROM + filter.sql()
                        + " order by o.created_at desc, o.id desc limit ? offset ?",
                SUMMARY_MAPPER,
                parameters.toArray());
        long totalElements = jdbc.queryForObject(
                "select count(*) " + ORDER_FROM + filter.sql(),
                Long.class,
                filter.parameters().toArray());
        int totalPages = Math.toIntExact((totalElements + size - 1) / size);
        return new SalesOrderPage<>(content, page, size, totalElements, totalPages);
    }

    public Optional<SalesOrderDetails> findDetails(long id) {
        List<SalesOrderDetails> rows = jdbc.query(
                ORDER_SELECT + ORDER_FROM + " where o.id = ?",
                DETAILS_MAPPER,
                id);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        SalesOrderDetails order = rows.getFirst();
        List<SalesOrderLineDetails> items = jdbc.query(
                """
                select id, sku_name_snapshot as product_name, sku_code_snapshot as product_sku,
                       quantity, unit_price, total_price as line_total
                from app.hm_order_item
                where order_id = ?
                order by id
                """,
                LINE_MAPPER,
                id);
        return Optional.of(new SalesOrderDetails(
                order.id(),
                order.orderNumber(),
                order.channel(),
                order.status(),
                order.paymentStatus(),
                order.paymentMethod(),
                order.customerName(),
                order.customerEmail(),
                order.customerPhone(),
                order.shippingAddress(),
                order.subtotal(),
                order.discountAmount(),
                order.shippingFee(),
                order.totalAmount(),
                order.placedAt(),
                items));
    }

    public int confirmIfPending(long id) {
        return jdbc.update(
                """
                update app.hm_order
                set status = 'CONFIRMED', version = version + 1
                where id = ? and status = 'PENDING_PAYMENT'
                """,
                id);
    }

    public Optional<String> findStatus(long id) {
        List<String> statuses = jdbc.query(
                "select status from app.hm_order where id = ?",
                (row, rowNumber) -> row.getString("status"),
                id);
        return statuses.stream().findFirst();
    }

    private Filter filter(SalesOrderSearch search) {
        StringBuilder sql = new StringBuilder(" where true");
        List<Object> parameters = new ArrayList<>();
        if (search.query() != null && !search.query().isBlank()) {
            String pattern = "%" + escapeLike(search.query().trim().toLowerCase(Locale.ROOT)) + "%";
            sql.append("""
                     and (lower(o.order_code) like ? escape '!'
                          or lower(u.full_name) like ? escape '!'
                          or lower(u.email) like ? escape '!'
                          or lower(u.phone) like ? escape '!'
                          or lower(sh.recipient_name) like ? escape '!'
                          or lower(sh.recipient_phone) like ? escape '!')
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (search.channel() != null) {
            sql.append(" and o.order_type = ?");
            parameters.add(search.channel().name());
        }
        if (search.status() != null && !search.status().isBlank()) {
            sql.append(" and o.status = ?");
            parameters.add(search.status().trim());
        }
        if (search.paymentStatus() != null && !search.paymentStatus().isBlank()) {
            sql.append(" and o.payment_status = ?");
            parameters.add(search.paymentStatus().trim());
        }
        if (search.from() != null) {
            sql.append(" and o.created_at >= ?");
            parameters.add(Timestamp.from(
                    search.from().atStartOfDay(BUSINESS_TIME_ZONE).toInstant()));
        }
        if (search.to() != null) {
            sql.append(" and o.created_at < ?");
            parameters.add(Timestamp.from(
                    search.to().plusDays(1).atStartOfDay(BUSINESS_TIME_ZONE).toInstant()));
        }
        return new Filter(sql.toString(), parameters);
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private record Filter(String sql, List<Object> parameters) {
    }
}
