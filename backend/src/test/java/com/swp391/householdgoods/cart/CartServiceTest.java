package com.swp391.householdgoods.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.catalog.application.CatalogException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CartServiceTest {
    private static final String EMAIL = "customer@example.com";
    private static final long CUSTOMER_ID = 7L;
    private static final long SKU_ID = 10L;
    private final CartRepository repository = mock(CartRepository.class);
    private final CartService service = new CartService(repository);

    private void allowCartChange(long existingQuantity, int availableStock) {
        when(repository.findActiveCustomerIds(EMAIL, true)).thenReturn(List.of(CUSTOMER_ID));
        when(repository.findQuantity(CUSTOMER_ID, SKU_ID)).thenReturn(existingQuantity);
        when(repository.findEligibleStock(SKU_ID)).thenReturn(List.of(availableStock));
        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of());
    }

    private CartService.Item item(long skuId, int quantity, String lineTotal, boolean available) {
        return new CartService.Item(skuId, 100L, "Product", "SKU", "SKU-" + skuId, "/image.svg",
                quantity, "100000", lineTotal, 8, true, available);
    }

    @Test
    void addingAnExistingSkuIncreasesItsQuantity() {
        allowCartChange(2, 8);

        service.save(EMAIL, SKU_ID, 3, true);

        verify(repository).saveQuantity(CUSTOMER_ID, SKU_ID, 5, EMAIL);
        verify(repository).findActiveCustomerIds(EMAIL, true);
    }

    @Test
    void updatingReplacesQuantityInsteadOfAddingToIt() {
        allowCartChange(2, 8);

        service.save(EMAIL, SKU_ID, 3, false);

        verify(repository).saveQuantity(CUSTOMER_ID, SKU_ID, 3, EMAIL);
    }

    @Test
    void invalidAndExcessiveQuantitiesDoNotWriteToTheCart() {
        allowCartChange(2, 8);

        for (int quantity : new int[] {0, -1, 7, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> service.save(EMAIL, SKU_ID, quantity, true))
                    .isInstanceOfSatisfying(CatalogException.class,
                            error -> assertThat(error.code()).isEqualTo("CART_QUANTITY_INVALID"));
        }
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void ineligibleSkuIsRejectedWithoutSaving() {
        allowCartChange(1, 8);
        when(repository.findEligibleStock(SKU_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.save(EMAIL, SKU_ID, 1, true))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_SKU_INELIGIBLE"));
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void updatingAnAbsentLineDoesNotCreateIt() {
        allowCartChange(0, 8);

        assertThatThrownBy(() -> service.save(EMAIL, SKU_ID, 1, false))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_ITEM_NOT_FOUND"));
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void viewingUsesCurrentLineTotalsAndChecksEveryLineForCheckout() {
        when(repository.findActiveCustomerIds(EMAIL, false)).thenReturn(List.of(CUSTOMER_ID));
        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of(
                item(10, 2, "200000", true), item(11, 3, "300000", false)));

        CartService.Cart cart = service.view(EMAIL);

        assertThat(cart.subtotal()).isEqualTo("500000");
        assertThat(cart.checkoutEligible()).isFalse();
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void checkoutRequiresANonEmptyCartWithAllLinesAvailable() {
        when(repository.findActiveCustomerIds(EMAIL, false)).thenReturn(List.of(CUSTOMER_ID));
        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of());
        assertThat(service.view(EMAIL).checkoutEligible()).isFalse();
        assertThat(service.view(EMAIL).subtotal()).isEqualTo("0");

        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of(item(10, 2, "200000", true)));
        assertThat(service.view(EMAIL).checkoutEligible()).isTrue();
    }

    @Test
    void removingSeveralItemsAlwaysUsesTheAuthenticatedCustomer() {
        when(repository.findActiveCustomerIds(EMAIL, true)).thenReturn(List.of(CUSTOMER_ID));
        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of());

        CartService.Cart cart = service.remove(EMAIL, List.of(10L, 11L));

        verify(repository).deleteItem(CUSTOMER_ID, 10L);
        verify(repository).deleteItem(CUSTOMER_ID, 11L);
        assertThat(cart.items()).isEmpty();
        assertThat(cart.subtotal()).isEqualTo("0");
    }

    @Test
    void expiredSessionIsRejectedBeforeReadingCartContents() {
        when(repository.findActiveCustomerIds(EMAIL, false)).thenReturn(List.of());

        assertThatThrownBy(() -> service.view(EMAIL))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_SESSION_EXPIRED"));
        verify(repository, never()).findItems(anyLong());
    }

    @Test
    void readFailuresAreNotSwallowedAfterSaving() {
        allowCartChange(2, 8);
        when(repository.findItems(CUSTOMER_ID)).thenThrow(new DataAccessResourceFailureException("failed"));

        // Lỗi phải thoát khỏi Service để Spring rollback transaction, không trả thành công giả.
        assertThatThrownBy(() -> service.save(EMAIL, SKU_ID, 1, true))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
