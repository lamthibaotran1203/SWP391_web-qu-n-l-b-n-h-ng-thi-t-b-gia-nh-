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

    @Test void detailContainsRealMetadataAndOnlyEligibleSkusWithoutChangingStock() throws Exception {
        seed();
        long productId = product();
        jdbc.update("update app.product set description='Đáy dày, dùng bếp từ', image_url='/cover.svg' where product_id=?", productId);
        jdbc.update("update app.hm_sku set image_url='/two-litres.svg' where id=?", sku());
        jdbc.update("insert into app.hm_sku(product_id,sku_code,barcode,sku_name,original_price,selling_price,status,image_url,created_at) values (?, 'DETAIL-SIBLING','DETAIL-SIBLING','4 lít',300000,250000,'ACTIVE','/four-litres.svg',now())", productId);
        jdbc.update("insert into app.hm_sku(product_id,sku_code,barcode,sku_name,original_price,selling_price,status,image_url,created_at) values (?, 'DETAIL-HIDDEN','DETAIL-HIDDEN','Ẩn',1,1,'INACTIVE','/hidden.svg',now())", productId);
        mvc.perform(get("/api/v1/catalog/products/" + productId + "/detail"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productId").value(productId))
            .andExpect(jsonPath("$.data.description").value("Đáy dày, dùng bếp từ"))
            .andExpect(jsonPath("$.data.brandName").value("HomeMart Demo"))
            .andExpect(jsonPath("$.data.images.length()").value(3))
            .andExpect(jsonPath("$.data.skus.length()").value(2))
            .andExpect(jsonPath("$.data.skus[0].availableQuantity").value(5))
            .andExpect(jsonPath("$.data.skus[1].sellingPrice").value("250000"))
            .andExpect(jsonPath("$.data.skus[1].purchasable").value(false))
            .andExpect(jsonPath("$.data.legacyPrice").doesNotExist());
        assertThat(jdbc.queryForObject("select reserved_quantity from app.hm_inventory where sku_id=?", Integer.class, sku())).isZero();
        jdbc.update("update app.product set status='INACTIVE' where product_id=?", productId);
        mvc.perform(get("/api/v1/catalog/products/" + productId + "/detail")).andExpect(status().isNotFound());
        jdbc.update("update app.product set status='ACTIVE' where product_id=?", productId);
        jdbc.update("update app.hm_sku set status='INACTIVE' where product_id=?", productId);
        mvc.perform(get("/api/v1/catalog/products/" + productId + "/detail")).andExpect(status().isNotFound());
    }

    @Test void publicApiUsesCanonicalSourcesAndStableSaleUnitPagination() throws Exception {
        seed(); seed();
        jdbc.update("update app.hm_sku set selling_price=180000 where id=?",sku());
        jdbc.update("update app.hm_inventory set reserved_quantity=2 where sku_id=?",sku());
        mvc.perform(get("/api/v1/catalog/skus")
                .param("sort", "oldest")
                .param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].sellingPrice").value("180000"))
            .andExpect(jsonPath("$.data.items[0].availableQuantity").value(3))
            .andExpect(jsonPath("$.data.items[0].purchasable").value(true))
            .andExpect(jsonPath("$.data.items[0].legacyPrice").doesNotExist());
        mvc.perform(get("/api/v1/catalog/skus")
                .param("sort", "oldest")
                .param("page", "1")
                .param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].availableQuantity").value(0))
            .andExpect(jsonPath("$.data.items[0].purchasable").value(false));
        // Original fixture plus three photo products; only three have positive opening stock.
        assertThat(jdbc.queryForObject("select count(*) from app.hm_stock_movement where posting_key like 'DEMO_OPENING_%'",Integer.class)).isEqualTo(3);
        jdbc.update("delete from app.hm_inventory where sku_id=?",sku());
        mvc.perform(get("/api/v1/catalog/skus/"+sku()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.availableQuantity").value(0));
    }
    @Test void searchFiltersAndProductNameSortingUseCanonicalPublicData() throws Exception {
        seed();
        mvc.perform(get("/api/v1/catalog/skus").param("search", "SHD1182"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].skuCode").value("DEMO-SHD1182"));
        mvc.perform(get("/api/v1/catalog/skus")
                .param("brand", "SUNHOUSE").param("category", "Ấm siêu tốc")
                .param("minPrice", "200000").param("maxPrice", "300000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].productName").value("Ấm siêu tốc SUNHOUSE SHD1182 1.8 lít"));
        mvc.perform(get("/api/v1/catalog/skus").param("brand", "SUNHOUSE").param("sort", "name_asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].productName").value("Bộ nồi SUNHOUSE ECO SHG1122MX"));
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
        mvc.perform(get("/api/v1/catalog/skus").param("brand","HomeMart Demo")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/v1/catalog/skus")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(3));
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
