package com.swp391.householdgoods.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.swp391.householdgoods.catalog.application.CatalogException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class CartTransactionIntegrationTest {
    @Autowired CartService service;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean CartRepository repository;
    private String email;
    private long customerId;
    private long skuId;

    @BeforeEach
    void seedCommittedData() throws Exception {
        try (var stream = getClass().getResourceAsStream("/demo/catalog.sql")) {
            jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        email = "cart-transaction-" + UUID.randomUUID() + "@example.invalid";
        customerId = jdbc.queryForObject("""
                insert into app.user_account(email,password_hash,full_name,role,status,
                  email_verified_at,created_at,updated_at)
                values (?,'test','Cart Transaction','CUSTOMER','ACTIVE',now(),now(),now()) returning user_id
                """, Long.class, email);
        skuId = jdbc.queryForObject("select id from app.hm_sku where sku_code='DEMO-SKU-1'", Long.class);
    }

    @AfterEach
    void cleanOnlyThisCustomersData() {
        reset(repository);
        jdbc.update("delete from app.hm_cart_add_request where user_id=?", customerId);
        jdbc.update("delete from app.hm_cart where user_id=?", customerId);
        jdbc.update("delete from app.user_account where user_id=?", customerId);
    }

    @Test
    void addAndItsReceiptRollBackTogetherWhenTheSummaryCannotLoad() {
        doThrow(new DataAccessResourceFailureException("load failed")).when(repository).findItems(customerId);
        assertThatThrownBy(() -> service.addItem(email, skuId, 1, UUID.randomUUID()))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart where user_id=?", Integer.class, customerId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart_add_request where user_id=?", Integer.class, customerId)).isZero();
    }

    @Test
    void deletionStaysCommittedWhenLoadingTheRemainingCartFails() {
        service.addItem(email, skuId, 1, UUID.randomUUID());
        int reservedBefore = jdbc.queryForObject("select reserved_quantity from app.hm_inventory where sku_id=?", Integer.class, skuId);
        doThrow(new DataAccessResourceFailureException("load failed")).when(repository).findItems(customerId);

        var result = service.remove(email, List.of(skuId));

        assertThat(result.removed()).isTrue();
        assertThat(result.refreshRequired()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart where user_id=?", Integer.class, customerId)).isZero();
        assertThat(jdbc.queryForObject("select reserved_quantity from app.hm_inventory where sku_id=?", Integer.class, skuId)).isEqualTo(reservedBefore);
    }

    @Test
    void failureOnTheSecondDeletionRollsBackTheFirstDeletion() {
        service.addItem(email, skuId, 1, UUID.randomUUID());
        long secondSku = jdbc.queryForObject("select id from app.hm_sku where id<>? order by id limit 1", Long.class, skuId);
        jdbc.update("insert into app.hm_cart(user_id,sku_id,quantity,created_at) values (?,?,1,now())", customerId, secondSku);
        doThrow(new DataAccessResourceFailureException("delete failed")).when(repository).deleteItem(customerId, secondSku);

        assertThatThrownBy(() -> service.remove(email, List.of(skuId, secondSku)))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart where user_id=?", Integer.class, customerId)).isEqualTo(2);
    }

    @Test
    void twoConcurrentRetriesOfOneRequestOnlyAddOnce() throws Exception {
        UUID requestId = UUID.randomUUID();
        runTogether(() -> service.addItem(email, skuId, 2, requestId),
                () -> service.addItem(email, skuId, 2, requestId));
        assertThat(service.view(email).items().getFirst().quantity()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_cart where user_id=?", Integer.class, customerId)).isEqualTo(1);
    }

    @Test
    void distinctConcurrentAddsDoNotLoseEitherIncrement() throws Exception {
        runTogether(() -> service.addItem(email, skuId, 1, UUID.randomUUID()),
                () -> service.addItem(email, skuId, 1, UUID.randomUUID()));
        assertThat(service.view(email).items().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void twoConcurrentUpdatesFromTheSameRevisionCannotOverwriteEachOther() throws Exception {
        UUID version = service.addItem(email, skuId, 2, UUID.randomUUID()).items().getFirst().version();
        var outcomes = new java.util.concurrent.ConcurrentLinkedQueue<String>();
        runTogether(() -> updateWithOutcome(3, version, outcomes),
                () -> updateWithOutcome(4, version, outcomes));
        assertThat(outcomes).containsExactlyInAnyOrder("saved", "CART_CHANGED");
        assertThat(service.view(email).items().getFirst().quantity()).isIn(3, 4);
    }

    private void updateWithOutcome(int quantity, UUID version, java.util.Queue<String> outcomes) {
        try {
            service.updateItem(email, skuId, quantity, version);
            outcomes.add("saved");
        } catch (CatalogException failure) {
            outcomes.add(failure.code());
        }
    }

    private void runTogether(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> awaitStart(ready, start, first));
            var secondResult = executor.submit(() -> awaitStart(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            firstResult.get(15, TimeUnit.SECONDS);
            secondResult.get(15, TimeUnit.SECONDS);
        }
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start, Runnable operation) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
            operation.run();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failure);
        }
    }
}
