package com.swp391.householdgoods.catalog.brand.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.catalog.brand.application.dto.BrandResponse;
import com.swp391.householdgoods.catalog.brand.application.dto.CreateBrandRequest;
import com.swp391.householdgoods.catalog.brand.application.dto.UpdateBrandRequest;
import com.swp391.householdgoods.catalog.brand.domain.Brand;
import com.swp391.householdgoods.catalog.brand.infrastructure.BrandRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository repository;

    @InjectMocks
    private BrandService service;

    @Test
    void createBrandSavesWhenNameIsUnique() {
        CreateBrandRequest request = new CreateBrandRequest(
                "  Sunhouse  ",
                "Vietnam",
                "Gia dung Viet Nam",
                "https://example.com/logo.png"
        );

        when(repository.existsByNameIgnoreCase("Sunhouse")).thenReturn(false);
        when(repository.saveAndFlush(any(Brand.class))).thenAnswer(invocation -> {
            Brand b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BrandResponse response = service.createBrand(request, "manager@homemart.vn");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Sunhouse");
        assertThat(response.country()).isEqualTo("Vietnam");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.createdBy()).isEqualTo("manager@homemart.vn");
    }

    @Test
    void createBrandRejectsDuplicateNameCaseInsensitively() {
        CreateBrandRequest request = new CreateBrandRequest(
                "sunhouse",
                "Vietnam",
                null,
                null
        );

        when(repository.existsByNameIgnoreCase("sunhouse")).thenReturn(true);

        assertThatThrownBy(() -> service.createBrand(request, "manager@homemart.vn"))
                .isInstanceOf(BrandDuplicateException.class)
                .hasMessageContaining("Tên thương hiệu đã tồn tại: sunhouse");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createBrandHandlesConcurrentDatabaseUniqueConstraintViolation() {
        CreateBrandRequest request = new CreateBrandRequest(
                "Lock&Lock",
                "Korea",
                null,
                null
        );

        when(repository.existsByNameIgnoreCase("Lock&Lock")).thenReturn(false);
        when(repository.saveAndFlush(any(Brand.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> service.createBrand(request, "manager@homemart.vn"))
                .isInstanceOf(BrandDuplicateException.class)
                .hasMessageContaining("Tên thương hiệu đã tồn tại: Lock&Lock");
    }

    @Test
    void updateBrandUpdatesPermittedFields() {
        Brand brand = new Brand("Philips", "Netherlands", "Old description", null, "admin");
        brand.setId(2L);

        when(repository.findById(2L)).thenReturn(Optional.of(brand));
        when(repository.existsByNameIgnoreCaseAndIdNot("Philips Vietnam", 2L)).thenReturn(false);
        when(repository.saveAndFlush(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBrandRequest request = new UpdateBrandRequest(
                "  Philips Vietnam  ",
                "Vietnam",
                "New description",
                "https://example.com/philips.svg"
        );

        BrandResponse response = service.updateBrand(2L, request, "manager@homemart.vn");

        assertThat(response.name()).isEqualTo("Philips Vietnam");
        assertThat(response.country()).isEqualTo("Vietnam");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.logoUrl()).isEqualTo("https://example.com/philips.svg");
        assertThat(response.updatedBy()).isEqualTo("manager@homemart.vn");
    }

    @Test
    void updateBrandRejectsNonexistentId() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        UpdateBrandRequest request = new UpdateBrandRequest("Any", null, null, null);

        assertThatThrownBy(() -> service.updateBrand(999L, request, "manager@homemart.vn"))
                .isInstanceOf(BrandNotFoundException.class)
                .hasMessageContaining("Không tìm thấy thương hiệu với ID: 999");
    }

    @Test
    void updateBrandRejectsDuplicateNameOwnedByAnotherBrand() {
        Brand brand = new Brand("Philips", "Netherlands", null, null, "admin");
        brand.setId(2L);

        when(repository.findById(2L)).thenReturn(Optional.of(brand));
        when(repository.existsByNameIgnoreCaseAndIdNot("Sunhouse", 2L)).thenReturn(true);

        UpdateBrandRequest request = new UpdateBrandRequest("Sunhouse", null, null, null);

        assertThatThrownBy(() -> service.updateBrand(2L, request, "manager@homemart.vn"))
                .isInstanceOf(BrandDuplicateException.class)
                .hasMessageContaining("Tên thương hiệu đã tồn tại: Sunhouse");
    }

    @Test
    void updateBrandRejectsUpdatingDeactivatedBrand() {
        Brand brand = new Brand("Old Brand", "Vietnam", null, null, "admin");
        brand.setId(3L);
        brand.setDeleted(true);

        when(repository.findById(3L)).thenReturn(Optional.of(brand));

        UpdateBrandRequest request = new UpdateBrandRequest("New Name", null, null, null);

        assertThatThrownBy(() -> service.updateBrand(3L, request, "manager@homemart.vn"))
                .isInstanceOf(BrandDeactivatedException.class)
                .hasMessageContaining("Không thể cập nhật thương hiệu đã bị vô hiệu hóa.");
    }

    @Test
    void deactivateBrandSoftDeletesWhenNoActiveProducts() {
        Brand brand = new Brand("Toshiba", "Japan", null, null, "admin");
        brand.setId(5L);
        brand.setDeleted(false);

        when(repository.findById(5L)).thenReturn(Optional.of(brand));
        when(repository.countActiveProductsByBrandId(5L)).thenReturn(0L);
        when(repository.saveAndFlush(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = service.deactivateBrand(5L, "manager@homemart.vn");

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.updatedBy()).isEqualTo("manager@homemart.vn");
        verify(repository).saveAndFlush(brand);
    }

    @Test
    void deactivateBrandIsIdempotentWhenAlreadyDeactivated() {
        Brand brand = new Brand("Toshiba", "Japan", null, null, "admin");
        brand.setId(5L);
        brand.setDeleted(true);

        when(repository.findById(5L)).thenReturn(Optional.of(brand));

        BrandResponse response = service.deactivateBrand(5L, "manager@homemart.vn");

        assertThat(response.isDeleted()).isTrue();
        verify(repository, never()).countActiveProductsByBrandId(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateBrandRejectsWhenActiveProductsExist() {
        Brand brand = new Brand("Panasonic", "Japan", null, null, "admin");
        brand.setId(6L);
        brand.setDeleted(false);

        when(repository.findById(6L)).thenReturn(Optional.of(brand));
        when(repository.countActiveProductsByBrandId(6L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deactivateBrand(6L, "manager@homemart.vn"))
                .isInstanceOf(BrandHasActiveProductsException.class)
                .hasMessageContaining("vẫn còn 3 sản phẩm đang hoạt động");

        assertThat(brand.isDeleted()).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void searchBrandsFiltersCorrectly() {
        Brand b1 = new Brand("Sunhouse", "Vietnam", null, null, "admin");
        b1.setId(1L);

        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.searchBrands("Sun", false, pageable))
                .thenReturn(new PageImpl<>(List.of(b1), pageable, 1));

        Page<BrandResponse> page = service.searchBrands("  Sun  ", "ACTIVE", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().name()).isEqualTo("Sunhouse");
    }

    @Test
    void getBrandByIdReturnsBrandOrThrowsNotFound() {
        Brand brand = new Brand("Sharp", "Japan", null, null, "admin");
        brand.setId(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(brand));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getBrandById(10L).name()).isEqualTo("Sharp");
        assertThatThrownBy(() -> service.getBrandById(99L))
                .isInstanceOf(BrandNotFoundException.class);
    }
}
