package com.swp391.householdgoods.catalog.management;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.management.api.*;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.*;
import com.swp391.householdgoods.catalog.management.application.CatalogManagementService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogManagementIntegrationTest {
    private static final String API = "/api/v1/manager/catalog/products";
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired CatalogManagementService service;
    private String prefix;
    private String email;
    private long accountId;
    private long categoryId;
    private long brandId;

    @BeforeEach void fixtures() {
        prefix = "UC13-" + UUID.randomUUID();
        email = prefix + "@example.invalid";
        accountId = jdbc.queryForObject("insert into app.user_account(email,password_hash,full_name,role,status,"
                + "email_verified_at,created_at,updated_at) values (?,?,'Quản lý kiểm thử','MANAGER','ACTIVE',now(),now(),now())"
                + " returning user_id", Long.class, email, encoder.encode("local test password"));
        categoryId = jdbc.queryForObject("insert into app.hm_category(code,name,created_at) values (?,? ,now()) returning id",
                Long.class, prefix, prefix);
        brandId = jdbc.queryForObject("insert into app.hm_brand(name,created_at) values (?,now()) returning id", Long.class, prefix);
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from app.hm_order_item where sku_id in (select id from app.hm_sku where sku_code like ?)", prefix + "%");
        jdbc.update("delete from app.hm_order where order_code like ?", prefix + "%");
        jdbc.update("delete from app.hm_stock_movement where sku_id in (select id from app.hm_sku where sku_code like ?)", prefix + "%");
        jdbc.update("delete from app.hm_inventory where sku_id in (select id from app.hm_sku where sku_code like ?)", prefix + "%");
        jdbc.update("delete from app.hm_sku where product_id in (select product_id from app.product where name like ?)", prefix + "%");
        jdbc.update("delete from app.product where name like ?", prefix + "%");
        jdbc.update("delete from app.hm_category where id=?", categoryId);
        jdbc.update("delete from app.hm_brand where id=?", brandId);
        jdbc.update("delete from app.user_account where user_id=?", accountId);
    }

    @Test void createsMultipleManualSkusAtomicallyAndManagementIncludesDrafts() throws Exception {
        var session = login();
        var request = new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"), sku("B")));
        var result = write(session, post(API), request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skus.length()").value(2))
                .andExpect(jsonPath("$.data.skus[0].sellingPrice").value("200000"))
                .andExpect(jsonPath("$.data.skus[0].availableQuantity").value(0))
                .andExpect(jsonPath("$.data.skus[0].lowStock").value(true)).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).at("/data/productId").asLong();
        assertThat(jdbc.queryForObject("select legacy_sku from app.product where product_id=?", String.class, id)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from app.hm_inventory where sku_id in (select id from app.hm_sku where product_id=?)", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from app.hm_stock_movement where sku_id in (select id from app.hm_sku where product_id=?)", Long.class, id)).isZero();
        long skuId = service.detail(id).skus().getFirst().skuId();
        mvc.perform(get("/api/v1/catalog/skus/" + skuId)).andExpect(status().isOk());
        var draft = new ProductFields(prefix + " draft", null, null, null, null, ProductStatus.INACTIVE);
        var inactive = statusOf(sku("D"), ProductStatus.INACTIVE);
        var saved = service.create(new CreateProduct(draft, List.of(inactive)), email);
        mvc.perform(get(API).session(session).param("search", prefix).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].status").value("INACTIVE"));
        mvc.perform(get(API + "/" + saved.productId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/skus/" + saved.skus().getFirst().skuId())).andExpect(status().isNotFound());
    }

    @Test void duplicateCodeOrBarcodeRollsBackTheEntireNewProduct() {
        var fields = product(ProductStatus.ACTIVE);
        assertThatThrownBy(() -> service.create(new CreateProduct(fields, List.of(sku("A"), sku("A"))), email))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("SKU_CODE_DUPLICATE"));
        assertThat(productCount()).isZero();
        var saved = create();
        var duplicateBarcode = new SkuFields(prefix + "-B", saved.skus().getFirst().barcode(), "Đơn vị B", Map.of(), null,
                new BigDecimal("200000"), new BigDecimal("200000"), 1, ProductStatus.ACTIVE);
        assertThatThrownBy(() -> service.create(new CreateProduct(fields, List.of(sku("C"), duplicateBarcode)), email))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("SKU_BARCODE_DUPLICATE"));
        assertThat(productCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_sku where sku_code=?", Long.class, prefix + "-C")).isZero();
    }

    @Test void rejectsMissingPublicationDataDeletedReferencesAndInvalidValues() throws Exception {
        var session = login();
        var missing = new ProductFields(prefix, null, null, brandId, null, ProductStatus.ACTIVE);
        write(session, post(API), new CreateProduct(missing, List.of(sku("A")))).andExpect(status().isBadRequest());
        jdbc.update("update app.hm_category set is_deleted=true where id=?", categoryId);
        write(session, post(API), new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CATEGORY_INVALID"));
        jdbc.update("update app.hm_category set is_deleted=false where id=?", categoryId);
        jdbc.update("update app.hm_brand set is_deleted=true where id=?", brandId);
        write(session, post(API), new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BRAND_INVALID"));
        jdbc.update("update app.hm_brand set is_deleted=false where id=?", brandId);
        for (String price : List.of("-1", "10.5", "1000000000000000000")) {
            var invalid = priceOf(sku("A"), new BigDecimal(price));
            write(session, post(API), new CreateProduct(product(ProductStatus.ACTIVE), List.of(invalid)))
                    .andExpect(status().isBadRequest());
        }
        var blank = new ProductFields("  ", null, categoryId, brandId, "/test.svg", ProductStatus.ACTIVE);
        write(session, post(API), new CreateProduct(blank, List.of(sku("A")))).andExpect(status().isBadRequest());
        String raw = json.writeValueAsString(new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"))));
        write(session, post(API), raw.replace("\"minStockThreshold\":2", "\"minStockThreshold\":-1"))
                .andExpect(status().isBadRequest());
        write(session, post(API), raw.replace("\"capacity\":\"2 lít\"", "\"capacity\":\"   \""))
                .andExpect(status().isBadRequest());
        write(session, post(API), raw.replace("\"minStockThreshold\":2", "\"minStockThreshold\":1.5"))
                .andExpect(status().isBadRequest());
        assertThat(productCount()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"CUSTOMER", "STAFF"})
    void nonManagersCannotReadOrWriteManagementOrLegacyWrites(String role) throws Exception {
        jdbc.update("update app.user_account set role=? where user_id=?", role, accountId);
        var session = login();
        mvc.perform(get(API).session(session)).andExpect(status().isForbidden());
        write(session, post(API), new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"))))
                .andExpect(status().isForbidden());
        write(session, post("/api/v1/products"), "{}").andExpect(status().isForbidden());
    }

    @Test void anonymousCsrfAndRetiredWriteContractsAreEnforced() throws Exception {
        mvc.perform(get(API)).andExpect(status().isUnauthorized());
        var csrf = csrf(null);
        mvc.perform(post(API).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isUnauthorized());
        var session = login();
        mvc.perform(post(API).session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        write(session, post("/api/v1/products"), "{}").andExpect(status().isGone());
        mvc.perform(get(API).session(session).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(API).session(session).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(API).session(session).param("status", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test void editsAndDeactivationPreserveStockArchiveAndHistoricalOrderSnapshot() throws Exception {
        var saved = create();
        long id = saved.productId();
        long skuId = saved.skus().getFirst().skuId();
        jdbc.update("insert into app.hm_inventory(sku_id,physical_quantity,reserved_quantity,created_at) values (?,5,1,now())", skuId);
        long orderId = jdbc.queryForObject("insert into app.hm_order(order_code,order_type,status,payment_status,total_amount,final_amount,created_at)"
                + " values (?,'COUNTER','COMPLETED','PAID',200000,200000,now()) returning id", Long.class, prefix);
        jdbc.update("insert into app.hm_order_item(order_id,sku_id,quantity,unit_price,total_price,sku_code_snapshot,sku_name_snapshot,created_at)"
                + " values (?,?,1,200000,200000,?,'Tên lúc mua',now())", orderId, skuId, prefix + "-A");
        var session = login();
        var updated = write(session, put(API + "/" + id + "/skus/" + skuId),
                new EditSku(0L, 0L, priceOf(sku("A"), new BigDecimal("180000"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productVersion").value(1))
                .andExpect(jsonPath("$.data.skus[0].skuVersion").value(1)).andReturn();
        assertThat(jdbc.queryForObject("select available_quantity from app.hm_inventory where sku_id=?", Integer.class, skuId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select unit_price from app.hm_order_item where order_id=?", BigDecimal.class, orderId)).isEqualByComparingTo("200000");
        assertThat(jdbc.queryForObject("select legacy_price from app.product where product_id=?", BigDecimal.class, id)).isNull();
        write(session, put(API + "/" + id), new EditProduct(0L, product(ProductStatus.INACTIVE)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATALOG_STALE"));
        write(session, put(API + "/" + id), new EditProduct(1L, product(ProductStatus.INACTIVE))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/skus/" + skuId)).andExpect(status().isNotFound());
        assertThat(service.detail(id).skus()).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from app.hm_order_item where sku_id=?", Long.class, skuId)).isEqualTo(1);
    }

    @Test void addSkuAndSkuDeactivationRequireOwnedIdsAndFreshVersions() {
        var saved = create();
        var added = service.addSku(saved.productId(), new AddSku(0L, sku("B")), email);
        assertThat(added.skus()).hasSize(2);
        assertThatThrownBy(() -> service.addSku(saved.productId(), new AddSku(0L, sku("C")), email))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("CATALOG_STALE"));
        long skuId = saved.skus().getFirst().skuId();
        var deactivated = service.editSku(saved.productId(), skuId, new EditSku(1L, 0L, statusOf(sku("A"), ProductStatus.INACTIVE)), email);
        assertThat(deactivated.skus().getFirst().status()).isEqualTo(ProductStatus.INACTIVE);
        assertThatThrownBy(() -> service.editSku(saved.productId(), Long.MAX_VALUE, new EditSku(2L, 0L, sku("A")), email))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("SKU_NOT_FOUND"));
    }

    @Test void competingProductEditsPermitOneWinnerAndOneVersionConflict() throws Exception {
        var saved = create();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> edit = () -> {
                start.await();
                try { service.editProduct(saved.productId(), new EditProduct(0L, product(ProductStatus.INACTIVE))); return true; }
                catch (CatalogException conflict) { assertThat(conflict.code()).isEqualTo("CATALOG_STALE"); return false; }
            };
            var first = pool.submit(edit); var second = pool.submit(edit); start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(service.detail(saved.productId()).productVersion()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void databaseFailureOnSecondSkuRollsBackMasterAndEarlierSku() throws Exception {
        jdbc.execute("create function app.test_reject_uc13_sku() returns trigger language plpgsql as $$ begin "
                + "if new.sku_code like 'UC13-%-FAIL' then raise exception 'simulated catalog storage failure'; "
                + "end if; return new; end $$");
        jdbc.execute("create trigger test_uc13_write before insert on app.hm_sku for each row execute function app.test_reject_uc13_sku()");
        try {
            write(login(), post(API), new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"), sku("FAIL"))))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CATALOG_SAVE_UNAVAILABLE"));
            assertThat(productCount()).isZero();
            assertThat(jdbc.queryForObject("select count(*) from app.hm_sku where sku_code like ?", Long.class, prefix + "%")).isZero();
        } finally {
            jdbc.execute("drop trigger test_uc13_write on app.hm_sku");
            jdbc.execute("drop function app.test_reject_uc13_sku()");
        }
    }

    @Test void competingDuplicateCreatesLeaveExactlyOneCompleteProduct() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> create = () -> {
                start.await();
                try { create(); return true; }
                catch (CatalogException conflict) { assertThat(conflict.code()).isEqualTo("SKU_CODE_DUPLICATE"); return false; }
            };
            var first = pool.submit(create); var second = pool.submit(create); start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(productCount()).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from app.hm_sku where sku_code=?", Long.class, prefix + "-A")).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void canDeactivateWithRetainedDeletedReferencesButCannotPublishOrAssignThem() {
        var saved = create();
        jdbc.update("update app.hm_category set is_deleted=true where id=?", categoryId);
        jdbc.update("update app.hm_brand set is_deleted=true where id=?", brandId);
        var deactivated = service.editProduct(saved.productId(), new EditProduct(0L, product(ProductStatus.INACTIVE)));
        assertThat(deactivated.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThatThrownBy(() -> service.editProduct(saved.productId(), new EditProduct(1L, product(ProductStatus.ACTIVE))))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("CATEGORY_INVALID"));
        assertThatThrownBy(() -> service.create(new CreateProduct(product(ProductStatus.INACTIVE), List.of(statusOf(sku("B"), ProductStatus.INACTIVE))), email))
                .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("CATEGORY_INVALID"));
    }

    @Test void demotedManagerSessionCannotKeepItsOldManagementPermission() throws Exception {
        var session = login();
        jdbc.update("update app.user_account set role='STAFF' where user_id=?", accountId);
        mvc.perform(get(API).session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    private ManagedProduct create() { return service.create(new CreateProduct(product(ProductStatus.ACTIVE), List.of(sku("A"))), email); }
    private long productCount() { return jdbc.queryForObject("select count(*) from app.product where name like ?", Long.class, prefix + "%"); }
    private ProductFields product(ProductStatus status) { return new ProductFields(prefix, "Mặt hàng kiểm thử", categoryId, brandId, "/test.svg", status); }
    private SkuFields sku(String suffix) { return new SkuFields(prefix + "-" + suffix, prefix + "-BAR-" + suffix, "Dung tích " + suffix,
            Map.of("capacity", "2 lít"), null, new BigDecimal("220000"), new BigDecimal("200000"), 2, ProductStatus.ACTIVE); }
    private SkuFields priceOf(SkuFields fields, BigDecimal price) { return new SkuFields(fields.skuCode(), fields.barcode(), fields.skuName(),
            fields.attributes(), fields.imageUrl(), fields.originalPrice(), price, fields.minStockThreshold(), fields.status()); }
    private SkuFields statusOf(SkuFields fields, ProductStatus status) { return new SkuFields(fields.skuCode(), fields.barcode(), fields.skuName(),
            fields.attributes(), fields.imageUrl(), fields.originalPrice(), fields.sellingPrice(), fields.minStockThreshold(), status); }
    private ResultActions write(MockHttpSession session, MockHttpServletRequestBuilder request, Object body) throws Exception {
        var csrf = csrf(session);
        return mvc.perform(request.session(session).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content(body instanceof String raw ? raw : json.writeValueAsString(body)));
    }
    private MockHttpSession login() throws Exception {
        var csrf = csrf(null);
        return (MockHttpSession) mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .param("email", email).param("password", "local test password"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private Csrf csrf(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/csrf"); if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"), json.readTree(result.getResponse().getContentAsString()).at("/data/token").asText());
    }
    private record Csrf(Cookie cookie, String token) {}
}
