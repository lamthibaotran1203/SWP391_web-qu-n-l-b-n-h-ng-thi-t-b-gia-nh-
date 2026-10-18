package com.swp391.householdgoods.cart;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartIntegrationTest {
    @Autowired CartService service;
    @Autowired JdbcTemplate jdbc;
    private long seed(String email) throws Exception {
        try (var stream = getClass().getResourceAsStream("/demo/catalog.sql")) {
            jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        jdbc.update("insert into app.user_account(email,password_hash,full_name,role,status,email_verified_at,created_at,updated_at) values (?, 'test', 'Cart Test', 'CUSTOMER','ACTIVE',now(),now(),now())", email);
        return jdbc.queryForObject("select id from app.hm_sku where sku_code='DEMO-SKU-1'", Long.class);
    }
    @Test void mergesUpdatesReadsCurrentPricesAndRemovesWithoutReservingStock() throws Exception {
        long sku = seed("cart-test@example.com");
        assertThat(service.view("cart-test@example.com").items()).isEmpty();
        service.save("cart-test@example.com", sku, 2, true);
        var cart = service.save("cart-test@example.com", sku, 1, true);
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
        jdbc.update("update app.hm_sku set selling_price=123000 where id=?", sku);
        assertThat(service.view("cart-test@example.com").subtotal()).isEqualTo("369000");
        assertThatThrownBy(() -> service.save("cart-test@example.com", sku, 6, false)).hasMessageContaining("tồn kho");
        assertThat(service.view("cart-test@example.com").items().getFirst().quantity()).isEqualTo(3);
        jdbc.update("update app.hm_inventory set reserved_quantity=4 where sku_id=?", sku);
        assertThat(service.view("cart-test@example.com").checkoutEligible()).isFalse();
        service.save("cart-test@example.com", sku, 1, false);
        jdbc.update("update app.hm_sku set status='INACTIVE' where id=?", sku);
        assertThat(service.view("cart-test@example.com").items().getFirst().eligible()).isFalse();
        assertThatThrownBy(() -> service.save("cart-test@example.com", sku, 1, true)).hasMessageContaining("không còn");
        assertThat(service.remove("cart-test@example.com", List.of(sku)).items()).isEmpty();
        assertThat(jdbc.queryForObject("select reserved_quantity from app.hm_inventory where sku_id=?", Integer.class, sku)).isEqualTo(4);
    }
    @Test void customersCannotReadOrDeleteEachOthersItems() throws Exception {
        long sku = seed("cart-owner@example.com");
        seed("cart-other@example.com");
        service.save("cart-owner@example.com", sku, 1, true);
        assertThat(service.remove("cart-other@example.com", List.of(sku)).items()).isEmpty();
        assertThat(service.view("cart-owner@example.com").items()).hasSize(1);
    }
}
