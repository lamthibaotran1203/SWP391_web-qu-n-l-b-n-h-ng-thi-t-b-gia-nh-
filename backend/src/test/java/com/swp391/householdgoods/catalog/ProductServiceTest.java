package com.swp391.householdgoods.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.application.ProductService;
import com.swp391.householdgoods.catalog.application.dto.CreateProductRequest;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.infrastructure.ProductWriteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductWriteRepository repository;
    ProductService service;
    @BeforeEach void setup() { service = new ProductService(repository); }
    private CreateProductRequest request(int stock, Long version) {
        return new CreateProductRequest("SKU","Nồi inox","Brand","Category",new BigDecimal("200000"),stock,
                null,"/image.svg",ProductStatus.ACTIVE,version,version);
    }
    private ProductResponse current() {
        return new ProductResponse(42L,"SKU","Nồi inox","Brand","Category",new BigDecimal("200000"),5,null,
                "/image.svg",ProductStatus.ACTIVE,Instant.EPOCH,Instant.EPOCH,100L,2L,2L);
    }
    @Test void cannotSeedStockThroughCatalog() {
        assertThatThrownBy(() -> service.create(request(5,null))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
    @Test void staleUpdatesFailBeforeMutation() {
        when(repository.lock(42L)).thenReturn(current());
        assertThatThrownBy(() -> service.update(42L,request(5,1L))).isInstanceOf(CatalogException.class)
                .hasMessageContaining("Dữ liệu đã thay đổi");
        verify(repository,never()).update(any(),any(),any(),any(),any());
    }
    @Test void stockChangesFailBeforeMutation() {
        when(repository.lock(42L)).thenReturn(current());
        assertThatThrownBy(() -> service.update(42L,request(4,2L))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tồn kho");
        verify(repository,never()).update(any(),any(),any(),any(),any());
    }
    @Test void deleteRequiresBothCurrentVersions() {
        when(repository.lock(42L)).thenReturn(current());
        assertThatThrownBy(() -> service.delete(42L,null,null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete(42L,2L,1L)).isInstanceOf(CatalogException.class);
        verify(repository,never()).softDelete(anyLong());
        service.delete(42L,2L,2L);
        verify(repository).softDelete(42L);
    }
    @Test void duplicateIsReturnedAsConflict() {
        doThrow(new org.springframework.dao.DuplicateKeyException("duplicate")).when(repository).guardSku("SKU",null);
        assertThatThrownBy(() -> service.create(request(0,null))).isInstanceOf(CatalogException.class)
                .satisfies(error -> assertThat(((CatalogException)error).status().value()).isEqualTo(409));
        verify(repository,never()).insert(any(),any(),any(),any());
    }
}
