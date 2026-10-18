package com.swp391.householdgoods.orders.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SalesOrderConfirmationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/staff/orders/1/confirm").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerRoleIsForbidden() throws Exception {
        long orderId = createOrder("PENDING_PAYMENT", "PENDING");

        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("customer").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void staffCanConfirmAndPaymentStatusRemainsUnchanged() throws Exception {
        long orderId = createOrder("PENDING_PAYMENT", "PENDING");

        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        assertThat(statusOf(orderId)).isEqualTo("CONFIRMED");
        assertThat(paymentStatusOf(orderId)).isEqualTo("PENDING");
        assertThat(versionOf(orderId)).isEqualTo(1);
    }

    @Test
    void adminCanConfirm() throws Exception {
        long orderId = createOrder("PENDING_PAYMENT", "PAID");

        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }

    @Test
    void unknownOrderReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/staff/orders/999999/confirm")
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"})
    void nonPendingOrderReturnsConflictWithoutChangingState(String initialStatus) throws Exception {
        long orderId = createOrder(initialStatus, "PAID");

        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));

        assertThat(statusOf(orderId)).isEqualTo(initialStatus);
        assertThat(versionOf(orderId)).isZero();
    }

    @Test
    void confirmingAnOrderTwiceReturnsConflict() throws Exception {
        long orderId = createOrder("PENDING_PAYMENT", "PENDING");

        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/orders/{id}/confirm", orderId)
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertThat(versionOf(orderId)).isEqualTo(1);
    }

    private long createOrder(String orderStatus, String paymentStatus) {
        String orderCode = "CONFIRM-" + UUID.randomUUID();
        return jdbc.queryForObject(
                """
                insert into app.hm_order (
                    order_code, order_type, status, payment_status,
                    total_amount, discount_amount, shipping_fee, final_amount, created_at
                ) values (?, 'COUNTER', ?, ?, 1000, 0, 0, 1000, CURRENT_TIMESTAMP)
                returning id
                """,
                Long.class,
                orderCode,
                orderStatus,
                paymentStatus);
    }

    private String statusOf(long orderId) {
        return jdbc.queryForObject(
                "select status from app.hm_order where id = ?",
                String.class,
                orderId);
    }

    private String paymentStatusOf(long orderId) {
        return jdbc.queryForObject(
                "select payment_status from app.hm_order where id = ?",
                String.class,
                orderId);
    }

    private long versionOf(long orderId) {
        return jdbc.queryForObject(
                "select version from app.hm_order where id = ?",
                Long.class,
                orderId);
    }
}
