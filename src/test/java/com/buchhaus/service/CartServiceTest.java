package com.buchhaus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.buchhaus.domain.CartItem;
import com.buchhaus.web.dto.CartView;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CartServiceTest {

    private Fixture f;
    private CartService service;

    @BeforeEach
    void setUp() {
        f = new Fixture();
        service = new CartService(f.cartItems, f.books, f.users);
    }

    private static HttpStatus status(Throwable e) {
        return ((ApiException) e).getStatus();
    }

    @Test
    void addingBooksTotalsTheCartAndAppliesShipping() {
        CartView v = service.setQuantity(1L, 20L, 1);          // Ikigai 350
        assertThat(v.itemCount()).isEqualTo(1);
        assertThat(v.subtotal()).isEqualByComparingTo(new BigDecimal("350"));
        assertThat(v.shipping()).isEqualByComparingTo(new BigDecimal("49"));
        assertThat(v.total()).isEqualByComparingTo(new BigDecimal("399"));
        assertThat(v.freeShippingGap()).isEqualByComparingTo(new BigDecimal("149"));
        assertThat(v.canCheckout()).isTrue();

        v = service.setQuantity(1L, 13L, 1);                    // + Clean Code 799 -> free shipping
        assertThat(v.subtotal()).isEqualByComparingTo(new BigDecimal("1149"));
        assertThat(v.shipping()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(v.freeShippingGap()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void changingQuantityUpdatesTheSameLine() {
        service.setQuantity(1L, 13L, 1);
        CartView v = service.setQuantity(1L, 13L, 3);
        assertThat(v.items()).hasSize(1);
        assertThat(v.items().get(0).quantity()).isEqualTo(3);
        assertThat(v.items().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("2397"));
    }

    @Test
    void cannotAddMoreThanInStock() {
        assertThatThrownBy(() -> service.setQuantity(1L, 20L, 3))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Only 2 copies");
        assertThatThrownBy(() -> service.setQuantity(1L, 24L, 1)).hasMessageContaining("out of stock");
        verify(f.cartItems, never()).save(any());
    }

    @Test
    void quantityLimitsAndUnknownBooks() {
        assertThatThrownBy(() -> service.setQuantity(1L, 13L, 11)).hasMessageContaining("between 0 and 10");
        assertThatThrownBy(() -> service.setQuantity(1L, 99L, 1)).hasMessage("Book not found");
    }

    @Test
    void zeroRemovesTheLine() {
        f.inCart(f.cleanCode, 2);
        CartItem line = f.cart.get(0);
        service.setQuantity(1L, 13L, 0);
        verify(f.cartItems).delete(line);
    }

    @Test
    void cartFlagsLinesThatCanNoLongerBeBought() {
        f.inCart(f.ikigai, 2);
        f.ikigai.setStock(1);                    // someone else bought a copy meanwhile
        CartView v = service.view(1L);
        assertThat(v.items().get(0).inStock()).isFalse();
        assertThat(v.canCheckout()).isFalse();
    }

    @Test
    void emptyCart() {
        CartView v = service.view(1L);
        assertThat(v.items()).isEmpty();
        assertThat(v.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(v.canCheckout()).isFalse();
    }
}
