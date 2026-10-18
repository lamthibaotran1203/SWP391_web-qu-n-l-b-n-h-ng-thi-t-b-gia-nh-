package com.swp391.householdgoods.catalog;

import com.swp391.householdgoods.catalog.application.*;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService.Search;
import com.swp391.householdgoods.catalog.api.PublicCategory;
import com.swp391.householdgoods.catalog.infrastructure.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogueBrowseServiceTest {
    private final CatalogueBrowseRepository products=mock(CatalogueBrowseRepository.class);
    private final PublicCategoryRepository categories=mock(PublicCategoryRepository.class);
    private final CatalogueBrowseService service=new CatalogueBrowseService(products,categories);
    private Search search(String availability,String sort,BigDecimal min,BigDecimal max,int page) {
        return new Search(" nồi  inox ",null,null,min,max,availability,sort,page,12);
    }
    @Test void clampsTheServerPageAndNormalizesText() {
        var normalized=new Search("nồi inox",null,null,null,null,"any","newest",9,12);
        when(products.count(normalized)).thenReturn(13L);
        when(products.page(normalized,1)).thenReturn(List.of());
        var result=service.browse(search("any","newest",null,null,9));
        assertThat(result.page()).isEqualTo(1);
        verify(products).page(normalized,1);
    }
    @Test void rejectsInvalidFiltersBeforeQueryingProducts() {
        for (Search input : List.of(search("invalid","newest",null,null,0),search("any","bad",null,null,0),
                search("any","newest",new BigDecimal("1.5"),null,0),search("any","newest",BigDecimal.TEN,BigDecimal.ONE,0),
                search("any","newest",null,null,-1),new Search("x".repeat(101),null,null,null,null,"any","newest",0,12),
                new Search("",null,999L,null,null,"any","newest",0,12))) {
            assertThatThrownBy(() -> service.browse(input)).isInstanceOf(CatalogException.class);
        }
        verifyNoInteractions(products);
    }
    @Test void filtersRootsAndChildrenWithoutUsingProductEligibility() {
        when(categories.all()).thenReturn(List.of(new PublicCategory(1,null,"Parent",null,null,true),new PublicCategory(2,1L,"Child",null,null,false)));
        when(categories.visible(1)).thenReturn(true);
        assertThat(service.categories(null,true)).extracting(PublicCategory::id).containsExactly(1L);
        assertThat(service.categories(1L,false)).extracting(PublicCategory::id).containsExactly(2L);
        verifyNoInteractions(products);
    }
    @Test void br44RejectsInactiveCategoryAsProductFilterAndCategoryParent() {
        when(categories.visible(44L)).thenReturn(false);
        var input=new Search(null,null,44L,null,null,"any","newest",0,12);
        assertThatThrownBy(() -> service.browse(input)).isInstanceOfSatisfying(CatalogException.class, error -> {
            assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
            assertThat(error.code()).isEqualTo("CATALOG_CATEGORYID_INVALID");
        });
        assertThatThrownBy(() -> service.categories(44L,false)).isInstanceOf(CatalogException.class);
        verifyNoInteractions(products);
    }

    @Test void emptyCatalogueReturnsPageZeroAndEmptyNormalizedKeyword() {
        Search normalized = new Search("", null, null, null, null, "any", "newest", 20, 12);
        when(products.count(normalized)).thenReturn(0L);
        when(products.page(normalized, 0)).thenReturn(List.of());

        var result = service.browse(new Search(null, null, null, null, null, "any", "newest", 20, 12));

        assertThat(result.page()).isZero();
        assertThat(result.totalElements()).isZero();
        verify(products).page(normalized, 0);
    }

    @Test void acceptsWholeVndAndTheMaximumPriceWithoutChangingFilters() {
        when(categories.visible(1L)).thenReturn(true);
        when(categories.brandExists(2L)).thenReturn(true);
        Search input = new Search("pan", 2L, 1L, new BigDecimal("0.00"),
                new BigDecimal("999999999999999999"), "out_of_stock", "price_desc", 0, 100);
        when(products.count(input)).thenReturn(1L);
        when(products.page(input, 0)).thenReturn(List.of());

        service.browse(input);

        verify(products).page(input, 0);
    }

    @Test void rejectsPricesBeyondLimitsAndInvalidReferenceIdsBeforeProductQueries() {
        for (Search input : List.of(
                new Search("", null, null, new BigDecimal("-1"), null, "any", "newest", 0, 12),
                new Search("", null, null, null, new BigDecimal("1000000000000000000"), "any", "newest", 0, 12),
                new Search("", 0L, null, null, null, "any", "newest", 0, 12),
                new Search("", 9L, null, null, null, "any", "newest", 0, 12),
                new Search("", null, null, null, null, "any", "newest", 0, 101))) {
            assertThatThrownBy(() -> service.browse(input)).isInstanceOf(CatalogException.class);
        }
        verifyNoInteractions(products);
    }

    @Test void parentFilterTakesPrecedenceOverRootsAndAllCategoriesKeepTheirOrder() {
        var root = new PublicCategory(1, null, "Root", null, null, true);
        var child = new PublicCategory(2, 1L, "Child", null, null, false);
        when(categories.all()).thenReturn(List.of(root, child));
        when(categories.visible(1L)).thenReturn(true);

        assertThat(service.categories(null, false)).containsExactly(root, child);
        assertThat(service.categories(1L, true)).containsExactly(child);
    }
}
