package com.swp391.householdgoods.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
class ProductWriteIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private void seed() throws Exception {
        try (var stream=getClass().getResourceAsStream("/demo/catalog.sql")) {
            jdbc.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        }
    }

    private String body(String code, int stock, long productVersion, long skuVersion) throws Exception {
        String category=jdbc.queryForObject("select name from app.hm_category where code='DEMO-CATALOG'",String.class);
        return json.writeValueAsString(Map.of("sku",code,"name","Sản phẩm kiểm thử","brand","HomeMart Demo",
                "category",category,"price",190000,"stockQuantity",stock,"imageUrl","/images/catalog-demo.svg",
                "status","ACTIVE","productVersion",productVersion,"skuVersion",skuVersion));
    }

    @Test void createReadUpdateDeleteUseSkuIdentityAndPreserveArchivesAndStock() throws Exception {
        seed();
        // Force product ID != SKU ID to catch accidental use of product identity.
        jdbc.update("insert into app.product(name,created_at,updated_at) values ('Nháp kiểm thử',now(),now())");
        jdbc.queryForObject("select setval(pg_get_serial_sequence('app.product','product_id'),nextval(pg_get_serial_sequence('app.hm_sku','id'))+100)",Long.class);
        var result=mvc.perform(post("/api/v1/products").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("TEST-WRITE",0,0,0)))
                .andExpect(status().isCreated()).andReturn();
        var data=json.readTree(result.getResponse().getContentAsString()).get("data");
        long skuId=data.get("id").asLong(), productId=data.get("productId").asLong();
        assertThat(skuId).isNotEqualTo(productId);
        assertThat(jdbc.queryForObject("select legacy_price from app.product where product_id=?",java.math.BigDecimal.class,productId)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from app.hm_inventory where sku_id=?",Integer.class,skuId)).isZero();
        mvc.perform(get("/api/v1/products/"+skuId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.sku").value("TEST-WRITE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mvc.perform(put("/api/v1/products/"+skuId).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("TEST-EDIT",0,0,0))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuVersion").value(1));
        mvc.perform(get("/api/v1/catalog/skus/"+skuId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.skuCode").value("TEST-EDIT"));
        mvc.perform(put("/api/v1/products/"+skuId).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("TEST-EDIT",0,0,0))).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/products/"+skuId).param("productVersion","1").param("skuVersion","1")
                .with(user("manager").roles("MANAGER")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/products/"+skuId)).andExpect(status().isNotFound());
    }

    @Test void duplicateInvalidMoneyAndStockWritesDoNotBecome500() throws Exception {
        seed();
        mvc.perform(post("/api/v1/products").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("demo-sku-1",0,0,0))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/products").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("TEST-STOCK",1,0,0))).andExpect(status().isBadRequest());
        String fractional=body("TEST-PRICE",0,0,0).replace("190000","190000.5");
        mvc.perform(post("/api/v1/products").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(fractional)).andExpect(status().isBadRequest());
        long id=jdbc.queryForObject("select id from app.hm_sku where sku_code='DEMO-SKU-1'",Long.class);
        mvc.perform(put("/api/v1/products/"+id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType("application/json").content(body("DEMO-SKU-1",4,0,0))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select physical_quantity from app.hm_inventory where sku_id=?",Integer.class,id)).isEqualTo(5);
    }
}
