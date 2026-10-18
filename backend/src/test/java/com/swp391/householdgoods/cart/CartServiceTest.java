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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CartServiceTest {
    private static final String EMAIL = "customer@example.com";
    private static final long CUSTOMER_ID = 7L;
    private static final long SKU_ID = 10L;
    private final CartRepository repository = mock(CartRepository.class);
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private final CartRemovalService removalService = mock(CartRemovalService.class);
    private final CartService service = new CartService(repository, removalService);

    private void allowCartChange(long existingQuantity, int availableStock) {
        when(repository.findActiveCustomerIds(EMAIL, true)).thenReturn(List.of(CUSTOMER_ID));
        when(repository.findQuantity(CUSTOMER_ID, SKU_ID)).thenReturn(existingQuantity);
        when(repository.findLine(CUSTOMER_ID, SKU_ID)).thenReturn(existingQuantity == 0 ? List.of()
                : List.of(new CartRepository.Line((int) existingQuantity, VERSION)));
        when(repository.findEligibleStock(SKU_ID)).thenReturn(List.of(availableStock));
        when(repository.findItems(CUSTOMER_ID)).thenReturn(List.of());
    }

    private CartService.Item item(long skuId, int quantity, String lineTotal, boolean available) {
        return new CartService.Item(skuId, 100L, "Product", "SKU", "SKU-" + skuId, "/image.svg",
                quantity, "100000", lineTotal, 8, true, available, VERSION);
    }

    @Test
    void addingAnExistingSkuIncreasesItsQuantity() {
        allowCartChange(2, 8);

        service.addItem(EMAIL, SKU_ID, 3, REQUEST_ID);

        verify(repository).saveQuantity(CUSTOMER_ID, SKU_ID, 5, EMAIL);
        verify(repository).findActiveCustomerIds(EMAIL, true);
    }

    @Test
    void updatingReplacesQuantityInsteadOfAddingToIt() {
        allowCartChange(2, 8);

        service.updateItem(EMAIL, SKU_ID, 3, VERSION);

        verify(repository).saveQuantity(CUSTOMER_ID, SKU_ID, 3, EMAIL);
    }

    @Test
    void invalidAndExcessiveQuantitiesDoNotWriteToTheCart() {
        allowCartChange(2, 8);

        for (int quantity : new int[] {0, -1, 7, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> service.addItem(EMAIL, SKU_ID, quantity, REQUEST_ID))
                    .isInstanceOfSatisfying(CatalogException.class,
                            error -> assertThat(error.code()).isEqualTo("CART_QUANTITY_INVALID"));
        }
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void ineligibleSkuIsRejectedWithoutSaving() {
        allowCartChange(1, 8);
        when(repository.findEligibleStock(SKU_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.addItem(EMAIL, SKU_ID, 1, REQUEST_ID))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_SKU_INELIGIBLE"));
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void updatingAnAbsentLineDoesNotCreateIt() {
        allowCartChange(0, 8);

        assertThatThrownBy(() -> service.updateItem(EMAIL, SKU_ID, 1, VERSION))
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

        when(removalService.removeItems(EMAIL, List.of(10L, 11L))).thenReturn(CUSTOMER_ID);
        CartService.Cart cart = service.remove(EMAIL, List.of(10L, 11L)).cart();

        verify(removalService).removeItems(EMAIL, List.of(10L, 11L));
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
        assertThatThrownBy(() -> service.addItem(EMAIL, SKU_ID, 1, REQUEST_ID))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void replayedAddOnlyReadsTheCartWithoutAddingAgain() {
        allowCartChange(2, 8);
        when(repository.findAddRequest(CUSTOMER_ID, REQUEST_ID))
                .thenReturn(List.of(new CartRepository.AddRequest(SKU_ID, 3)));
        service.addItem(EMAIL, SKU_ID, 3, REQUEST_ID);
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
        verify(repository, never()).findEligibleStock(anyLong());
    }

    @Test
    void reusedRequestIdWithDifferentPayloadIsRejected() {
        allowCartChange(2, 8);
        when(repository.findAddRequest(CUSTOMER_ID, REQUEST_ID))
                .thenReturn(List.of(new CartRepository.AddRequest(SKU_ID, 1)));
        assertThatThrownBy(() -> service.addItem(EMAIL, SKU_ID, 3, REQUEST_ID))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_REQUEST_REUSED"));
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void unchangedQuantityDoesNotWriteOrRequireTheSkuToStillBeOnSale() {
        allowCartChange(2, 0);
        service.updateItem(EMAIL, SKU_ID, 2, VERSION);
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
        verify(repository, never()).findEligibleStock(anyLong());
    }

    @Test
    void staleVersionCannotOverwriteANewerQuantity() {
        allowCartChange(2, 8);
        assertThatThrownBy(() -> service.updateItem(EMAIL, SKU_ID, 3, UUID.randomUUID()))
                .isInstanceOfSatisfying(CatalogException.class,
                        error -> assertThat(error.code()).isEqualTo("CART_CHANGED"));
        verify(repository, never()).saveQuantity(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void missingLineIsRejectedBeforeLookingUpPublicSkuInformation() {
        allowCartChange(0, 8);
        assertThatThrownBy(() -> service.updateItem(EMAIL, SKU_ID, 1, VERSION))
                .isInstanceOf(CatalogException.class);
        verify(repository, never()).findEligibleStock(anyLong());
    }

    @Test
    void successfulRemovalIsConfirmedWhenReloadFails() {
        when(removalService.removeItems(EMAIL, List.of(SKU_ID))).thenReturn(CUSTOMER_ID);
        when(repository.findItems(CUSTOMER_ID)).thenThrow(new DataAccessResourceFailureException("failed"));
        var result = service.remove(EMAIL, List.of(SKU_ID));
        assertThat(result.removed()).isTrue();
        assertThat(result.refreshRequired()).isTrue();
        assertThat(result.cart()).isNull();
    }

    @Test
    void failedRemovalIsNotReportedAsSuccessful() {
        when(removalService.removeItems(EMAIL, List.of(SKU_ID)))
                .thenThrow(new DataAccessResourceFailureException("failed"));
        assertThatThrownBy(() -> service.remove(EMAIL, List.of(SKU_ID)))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verify(repository, never()).findItems(anyLong());
    }
}
