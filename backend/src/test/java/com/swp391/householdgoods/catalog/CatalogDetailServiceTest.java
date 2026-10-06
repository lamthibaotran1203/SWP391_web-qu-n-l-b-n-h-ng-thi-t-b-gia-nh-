package com.swp391.householdgoods.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.swp391.householdgoods.catalog.api.CatalogProductDetail;
import com.swp391.householdgoods.catalog.api.CatalogSku;
import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.catalog.infrastructure.CatalogRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogDetailServiceTest {
    @Test void invisibleProductCannotExposeMetadata() {
        CatalogRepository repository = mock(CatalogRepository.class);
        when(repository.byProduct(100)).thenReturn(List.of());
        assertThatThrownBy(() -> new CatalogQueryService(repository).productDetail(100))
                .isInstanceOf(CatalogException.class)
                .satisfies(error -> assertThat(((CatalogException) error).status().value()).isEqualTo(404));
        verify(repository, never()).productDetail(anyLong(), anyList());
    }

    @Test void metadataUsesExactlyThePublicEligibleSaleUnits() {
        CatalogRepository repository = mock(CatalogRepository.class);
        List<CatalogSku> skus = List.of(new CatalogSku(2, 100, "Nồi", "SKU-2", "2 lít", "/cover.svg",
                1, "Nhà bếp", 1, "HomeMart", "200000", 0, false, 0, 0));
        CatalogProductDetail detail = new CatalogProductDetail(100, "Nồi", "Mô tả", 1, "Nhà bếp",
                1, "HomeMart", List.of("/cover.svg"), skus);
        when(repository.byProduct(100)).thenReturn(skus);
        when(repository.productDetail(100, skus)).thenReturn(detail);
        assertThat(new CatalogQueryService(repository).productDetail(100)).isSameAs(detail);
        assertThat(detail.skus().getFirst().purchasable()).isFalse();
        assertThatThrownBy(() -> detail.images().add("/other.svg"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
