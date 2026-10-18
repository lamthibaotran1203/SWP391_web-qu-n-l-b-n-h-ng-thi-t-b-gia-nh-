package com.swp391.householdgoods.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CartRepositoryTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CartRepository repository = new CartRepository(jdbc);

    private void returnMappedRow(int stock, boolean eligible) throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getInt("quantity")).thenReturn(3);
        when(row.getInt("stock")).thenReturn(stock);
        when(row.getBigDecimal("selling_price")).thenReturn(new BigDecimal("590000"));
        when(row.getBoolean("eligible")).thenReturn(eligible);
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<CartService.Item>>any(), eq(7L)))
                .thenAnswer(invocation -> {
                    RowMapper<CartService.Item> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(row, 0));
                });
    }

    @Test
    void mapsCurrentPriceToExactWholeVndLineTotal() throws Exception {
        returnMappedRow(8, true);

        CartService.Item item = repository.findItems(7L).getFirst();

        assertThat(item.quantity()).isEqualTo(3);
        assertThat(item.unitPrice()).isEqualTo("590000");
        assertThat(item.lineTotal()).isEqualTo("1770000");
        assertThat(item.available()).isTrue();
    }

    @Test
    void availabilityRequiresBothEligibilityAndSufficientStock() throws Exception {
        returnMappedRow(2, true);
        assertThat(repository.findItems(7L).getFirst().available()).isFalse();

        returnMappedRow(8, false);
        assertThat(repository.findItems(7L).getFirst().available()).isFalse();
    }
}
