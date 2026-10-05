package com.swp391.householdgoods.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
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
class CatalogIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private void seed() throws Exception {
        try(var stream=getClass().getResourceAsStream("/demo/catalog.sql")) {
            jdbc.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        }
    }
    private long sku() { return jdbc.queryForObject("select id from app.hm_sku where sku_code='DEMO-SKU-1'",Long.class); }
    private long product() { return jdbc.queryForObject("select product_id from app.hm_sku where id=?",Long.class,sku()); }

    @Test void publicApiUsesCanonicalSourcesAndStableSaleUnitPagination() throws Exception {
        seed(); seed();
        jdbc.update("update app.hm_sku set selling_price=180000 where id=?",sku());
        jdbc.update("update app.hm_inventory set reserved_quantity=2 where sku_id=?",sku());
        mvc.perform(get("/api/v1/catalog/skus").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].sellingPrice").value("180000"))
            .andExpect(jsonPath("$.data.items[0].availableQuantity").value(3))
            .andExpect(jsonPath("$.data.items[0].purchasable").value(true))
            .andExpect(jsonPath("$.data.items[0].legacyPrice").doesNotExist());
        mvc.perform(get("/api/v1/catalog/skus").param("page","1").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].availableQuantity").value(0))
            .andExpect(jsonPath("$.data.items[0].purchasable").value(false));
        assertThat(jdbc.queryForObject("select count(*) from app.hm_stock_movement where posting_key like 'DEMO_OPENING_%'",Integer.class)).isEqualTo(1);
        jdbc.update("delete from app.hm_inventory where sku_id=?",sku());
        mvc.perform(get("/api/v1/catalog/skus/"+sku()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.availableQuantity").value(0));
    }
    @Test void hiddenProductsSkusAndDeletedReferencesNeverLeak() throws Exception {
        seed();
        jdbc.update("update app.product set status='INACTIVE' where product_id=?",product());
        mvc.perform(get("/api/v1/catalog/products/"+product())).andExpect(status().isNotFound());
        jdbc.update("update app.product set status='ACTIVE' where product_id=?",product());
        jdbc.update("update app.hm_sku set is_deleted=true where id=?",sku());
        mvc.perform(get("/api/v1/catalog/skus/"+sku())).andExpect(status().isNotFound());
        jdbc.update("update app.hm_sku set is_deleted=false where id=?",sku());
        jdbc.update("update app.hm_brand set is_deleted=true where name='HomeMart Demo'");
        mvc.perform(get("/api/v1/catalog/skus")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }
    @Test void invalidParametersAre400AndAnonymousWritesStayProtected() throws Exception {
        mvc.perform(get("/api/v1/catalog/skus").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/skus").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/skus").param("page","oops")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/skus/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/catalog/skus")).andExpect(status().isForbidden());
    }
    @Test void newProductsHaveNoLegacySourceAndArchiveCannotBePopulated() throws Exception {
        long id=jdbc.queryForObject("insert into app.product(name,created_at,updated_at) values ('Mặt hàng nháp',now(),now()) returning product_id",Long.class);
        assertThat(jdbc.queryForObject("select status from app.product where product_id=?",String.class,id)).isEqualTo("INACTIVE");
        assertThat(jdbc.queryForObject("select legacy_stock_quantity from app.product where product_id=?",Integer.class,id)).isNull();
        jdbc.execute("SAVEPOINT archive_guard");
        try {
            assertThatThrownBy(()->jdbc.update("update app.product set legacy_price=1 where product_id=?",id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        } finally { jdbc.execute("ROLLBACK TO SAVEPOINT archive_guard"); }
    }
}
