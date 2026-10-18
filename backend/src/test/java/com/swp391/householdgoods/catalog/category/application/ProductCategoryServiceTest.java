package com.swp391.householdgoods.catalog.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.catalog.category.application.dto.CategoryRequest;
import com.swp391.householdgoods.catalog.category.domain.ProductCategory;
import com.swp391.householdgoods.catalog.category.infrastructure.ProductCategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock ProductCategoryRepository repository;
    ProductCategoryService service;

    @BeforeEach
    void setUp() {
        service = new ProductCategoryService(repository);
    }

    @Test
    void createsNormalizedRootCategory() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(new CategoryRequest("  kitchen-tools ", "  Dụng cụ bếp  ", "  Tiện ích  ", null), "admin");

        assertThat(result.code()).isEqualTo("KITCHEN-TOOLS");
        assertThat(result.name()).isEqualTo("Dụng cụ bếp");
        assertThat(result.description()).isEqualTo("Tiện ích");
        verify(repository).saveAndFlush(any(ProductCategory.class));
    }

    @Test
    void returnsParentNameWhenCreatingChildCategory() {
        ProductCategory parent = category(9L, null, "HOME", "Gia dụng");
        when(repository.findById(9L)).thenReturn(Optional.of(parent));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(
                new CategoryRequest("KITCHEN", "Nhà bếp", null, 9L), "admin");

        assertThat(result.parentId()).isEqualTo(9L);
        assertThat(result.parentName()).isEqualTo("Gia dụng");
    }

    @Test
    void rejectsDuplicateCodeAndNameIndependently() {
        when(repository.existsByCodeIgnoreCase("KITCHEN")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CategoryRequest("kitchen", "Nhà bếp", null, null), "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class,
                        error -> assertThat(error.code()).isEqualTo("CATEGORY_CODE_DUPLICATE"));
    }

    @Test
    void rejectsSelfAndDescendantParents() {
        ProductCategory current = category(1L, null, "ROOT", "Gốc");
        ProductCategory child = category(2L, 1L, "CHILD", "Con");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findById(2L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.update(1L,
                new CategoryRequest("ROOT", "Gốc", null, 2L), "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class,
                        error -> assertThat(error.code()).isEqualTo("CATEGORY_CYCLE"));
    }

    @Test
    void preventsDeactivateWhenActiveProductsRemain() {
        ProductCategory category = category(7L, null, "COOK", "Nấu ăn");
        when(repository.findById(7L)).thenReturn(Optional.of(category));
        when(repository.countActiveProducts(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deactivate(7L, "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class, error -> {
                    assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(error.code()).isEqualTo("CATEGORY_HAS_ACTIVE_PRODUCTS");
                });
    }

    @Test
    void preventsDeactivateWhenActiveChildrenRemain() {
        ProductCategory category = category(7L, null, "COOK", "Nấu ăn");
        when(repository.findById(7L)).thenReturn(Optional.of(category));
        when(repository.countByParentIdAndDeletedFalse(7L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivate(7L, "admin"))
                .isInstanceOfSatisfying(CategoryManagementException.class,
                        error -> assertThat(error.code()).isEqualTo("CATEGORY_HAS_ACTIVE_CHILDREN"));
    }

    @Test
    void softDeactivatesCategoryWithoutDeletingIt() {
        ProductCategory category = category(7L, null, "COOK", "Nấu ăn");
        when(repository.findById(7L)).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.deactivate(7L, "admin@example.invalid");

        assertThat(result.isDeleted()).isTrue();
        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(category.getUpdatedBy()).isEqualTo("admin@example.invalid");
        verify(repository).saveAndFlush(category);
    }

    @Test
    void excludesCurrentCategoryAndItsDescendantsFromParentOptions() {
        ProductCategory root = category(1L, null, "ROOT", "Gốc");
        ProductCategory child = category(2L, 1L, "CHILD", "Con");
        ProductCategory other = category(3L, null, "OTHER", "Khác");
        when(repository.findAllByDeletedFalseOrderByNameAsc()).thenReturn(List.of(root, child, other));

        assertThat(service.parentOptions(1L)).extracting("id").containsExactly(3L);
    }

    private ProductCategory category(Long id, Long parentId, String code, String name) {
        ProductCategory category = new ProductCategory(parentId, code, name, null, "admin");
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
