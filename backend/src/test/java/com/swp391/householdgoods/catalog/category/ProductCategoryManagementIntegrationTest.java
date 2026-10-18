package com.swp391.householdgoods.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swp391.householdgoods.catalog.category.application.CategoryManagementException;
import com.swp391.householdgoods.catalog.category.application.ProductCategoryService;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryRequest;
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
class ProductCategoryManagementIntegrationTest {

    @Autowired ProductCategoryService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void managesHierarchyFromCreateThroughSoftDeactivation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var root = service.create(
                new CategoryRequest("ROOT-" + suffix, "Danh mục gốc " + suffix, null, null), "admin");
        var child = service.create(
                new CategoryRequest("CHILD-" + suffix, "Danh mục con " + suffix, "Mô tả", root.id()), "admin");

        assertThat(child.parentName()).isEqualTo(root.name());
        assertThat(service.search(suffix, "ACTIVE", 0, 10).getContent())
                .extracting("id").containsExactlyInAnyOrder(root.id(), child.id());
        assertThat(service.parentOptions(root.id())).extracting("id")
                .doesNotContain(root.id(), child.id());

        var updated = service.update(child.id(),
                new CategoryRequest(child.code(), "Danh mục con đã sửa " + suffix, null, root.id()), "admin");
        assertThat(updated.name()).contains("đã sửa");

        assertThatThrownBy(() -> service.deactivate(root.id(), "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class,
                        error -> assertThat(error.code()).isEqualTo("CATEGORY_HAS_ACTIVE_CHILDREN"));

        assertThat(service.deactivate(child.id(), "admin").isDeleted()).isTrue();
        assertThat(service.deactivate(root.id(), "admin").isDeleted()).isTrue();
        assertThat(service.get(child.id()).status()).isEqualTo("INACTIVE");
    }

    @Test
    void activeProductPreventsCategoryDeactivationAndKeepsHistory() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var category = service.create(
                new CategoryRequest("PRODUCT-" + suffix, "Có sản phẩm " + suffix, null, null), "admin");
        long brand = jdbc.queryForObject(
                "insert into app.hm_brand(name,created_at) values (?,current_timestamp) returning id",
                Long.class, "Nhãn UC11 " + suffix);
        jdbc.update("insert into app.product(name,status,category_id,brand_id,image_url,created_at,updated_at) "
                        + "values (?,'ACTIVE',?,?,?,current_timestamp,current_timestamp)",
                "Sản phẩm UC11 " + suffix, category.id(), brand, "/uc11-test.png");

        assertThatThrownBy(() -> service.deactivate(category.id(), "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class,
                        error -> assertThat(error.code()).isEqualTo("CATEGORY_HAS_ACTIVE_PRODUCTS"));
        assertThat(service.get(category.id()).isDeleted()).isFalse();
    }
}
