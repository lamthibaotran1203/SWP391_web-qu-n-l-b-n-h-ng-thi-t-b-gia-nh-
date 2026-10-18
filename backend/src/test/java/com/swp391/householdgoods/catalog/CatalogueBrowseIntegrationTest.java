package com.swp391.householdgoods.catalog;

import static org.assertj.core.api.Assertions.*;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService.Search;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogueBrowseIntegrationTest {
    @Autowired CatalogueBrowseService service;
    @Autowired JdbcTemplate jdbc;
    @Test void groupsProductsUsesPriceRangesAndHidesInactiveCategoryTrees() throws Exception {
        try(var stream=getClass().getResourceAsStream("/demo/catalog.sql")) { jdbc.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8)); }
        long product=jdbc.queryForObject("select product_id from app.hm_sku where sku_code='DEMO-SKU-1'",Long.class);
        long category=jdbc.queryForObject("select category_id from app.product where product_id=?",Long.class,product);
        jdbc.update("insert into app.hm_sku(product_id,sku_code,barcode,sku_name,original_price,selling_price,status,created_at) values (?,'UC08-EXTRA','UC08-EXTRA','Bigger',900000,900000,'ACTIVE',now())",product);
        var search=new Search(null,null,null,null,null,"any","newest",0,12);
        var matches=service.browse(search).items().stream().filter(p -> p.productId()==product).toList();
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().skuCount()).isGreaterThanOrEqualTo(2);
        assertThat(matches.getFirst().maxPrice()).isEqualTo("900000");
        var available=service.browse(new Search(null,null,null,null,null,"in_stock","newest",0,12));
        assertThat(available.items()).allMatch(p -> p.availableQuantity()>0);
        var empty=service.browse(new Search(null,null,null,null,null,"out_of_stock","newest",0,12));
        assertThat(empty.items()).allMatch(p -> p.availableQuantity()==0);
        long parent=jdbc.queryForObject("insert into app.hm_category(code,name,status,created_at) values ('UC08-PARENT','Parent','ACTIVE',now()) returning id",Long.class);
        jdbc.update("update app.hm_category set parent_id=? where id=?",parent,category);
        assertThat(service.browse(new Search(null,null,parent,null,null,"any","newest",0,12)).items()).anyMatch(p -> p.productId()==product);
        jdbc.update("update app.hm_category set status='INACTIVE' where id=?",parent);
        assertThat(service.categories(null,false)).noneMatch(c -> c.id()==category || c.id()==parent);
        assertThat(service.browse(search).items()).noneMatch(p -> p.productId()==product);
        assertThatThrownBy(() -> service.browse(new Search(null,null,parent,null,null,"any","newest",0,12)))
                .isInstanceOf(com.swp391.householdgoods.catalog.application.CatalogException.class);
    }
}
