package com.swp391.householdgoods.cart;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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
        service.addItem("cart-test@example.com", sku, 2, UUID.randomUUID());
        var cart = service.addItem("cart-test@example.com", sku, 1, UUID.randomUUID());
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
        jdbc.update("update app.hm_sku set selling_price=123000 where id=?", sku);
        assertThat(service.view("cart-test@example.com").subtotal()).isEqualTo("369000");
        assertThatThrownBy(() -> service.updateItem("cart-test@example.com", sku, 6, service.view("cart-test@example.com").items().getFirst().version())).hasMessageContaining("tồn kho");
        assertThat(service.view("cart-test@example.com").items().getFirst().quantity()).isEqualTo(3);
        jdbc.update("update app.hm_inventory set reserved_quantity=4 where sku_id=?", sku);
        assertThat(service.view("cart-test@example.com").checkoutEligible()).isFalse();
        service.updateItem("cart-test@example.com", sku, 1, service.view("cart-test@example.com").items().getFirst().version());
        jdbc.update("update app.hm_sku set status='INACTIVE' where id=?", sku);
        assertThat(service.view("cart-test@example.com").items().getFirst().eligible()).isFalse();
        assertThatThrownBy(() -> service.addItem("cart-test@example.com", sku, 1, UUID.randomUUID())).hasMessageContaining("không còn");
        assertThat(service.remove("cart-test@example.com", List.of(sku)).cart().items()).isEmpty();
        assertThat(jdbc.queryForObject("select reserved_quantity from app.hm_inventory where sku_id=?", Integer.class, sku)).isEqualTo(4);
    }
    @Test void customersCannotReadOrDeleteEachOthersItems() throws Exception {
        long sku = seed("cart-owner@example.com");
        seed("cart-other@example.com");
        service.addItem("cart-owner@example.com", sku, 1, UUID.randomUUID());
        assertThat(service.remove("cart-other@example.com", List.of(sku)).cart().items()).isEmpty();
        assertThat(service.view("cart-owner@example.com").items()).hasSize(1);
    }

    @Test void retriesNoOpAndStaleUpdatesRespectThePersistedRevision() throws Exception {
        String email = "cart-retry@example.com";
        long sku = seed(email);
        UUID request = UUID.randomUUID();
        var initial = service.addItem(email, sku, 2, request).items().getFirst();
        assertThat(service.addItem(email, sku, 2, request).items().getFirst().quantity()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart_add_request where request_id=?", Integer.class, request)).isEqualTo(1);
        var unchanged = service.updateItem(email, sku, 2, initial.version()).items().getFirst();
        assertThat(unchanged.version()).isEqualTo(initial.version());
        var updated = service.updateItem(email, sku, 3, initial.version()).items().getFirst();
        assertThat(updated.version()).isNotEqualTo(initial.version());
        assertThatThrownBy(() -> service.updateItem(email, sku, 4, initial.version()))
                .isInstanceOf(com.swp391.householdgoods.catalog.application.CatalogException.class);
        assertThat(service.view(email).items().getFirst().quantity()).isEqualTo(3);
    }

    @Test void inactiveCategoryOrParentMakesTheCartLineUnavailable() throws Exception {
        String email = "cart-hidden@example.com";
        long sku = seed(email);
        service.addItem(email, sku, 1, UUID.randomUUID());
        long category = jdbc.queryForObject("select category_id from app.product p join app.hm_sku s on p.product_id=s.product_id where s.id=?", Long.class, sku);
        long parent = jdbc.queryForObject("insert into app.hm_category(code,name,status,created_at) values ('CART-PARENT','Cart Parent','INACTIVE',now()) returning id", Long.class);
        jdbc.update("update app.hm_category set parent_id=? where id=?", parent, category);
        assertThat(service.view(email).items().getFirst().eligible()).isFalse();
        assertThatThrownBy(() -> service.addItem(email, sku, 1, UUID.randomUUID()))
                .hasMessageContaining("không còn");
        assertThat(service.view(email).items().getFirst().quantity()).isEqualTo(1);
    }
}
