package com.buchhaus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buchhaus.web.dto.BookRequest;
import com.buchhaus.web.dto.BookView;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BookServiceTest {

    private Fixture f;
    private BookService service;

    @BeforeEach
    void setUp() {
        f = new Fixture();
        service = new BookService(f.books, f.orderItems);
    }

    private static BookRequest request(String sku, String price, Long version) {
        return new BookRequest(sku, " Refactoring ", "Martin Fowler", "Technology", " ", new BigDecimal(price), 5,
                2018, 448, version);
    }

    @Test
    void createsBooksWithPriceInPaise() {
        BookView v = service.create(request("BH-0100", "649.50", null));
        assertThat(v.title()).isEqualTo("Refactoring");
        assertThat(v.price()).isEqualByComparingTo(new BigDecimal("649.50"));
        assertThat(v.description()).isNull();
    }

    @Test
    void duplicateSkuIsAConflict() {
        when(f.books.existsBySkuIgnoreCase("BH-0013")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("BH-0013", "100", null)))
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void subPaisaPricesAreRejected() {
        assertThatThrownBy(() -> service.create(request("BH-0101", "10.001", null))).hasMessageContaining("two decimals");
    }

    @Test
    void staleVersionIsRejectedSoEditsAreNotLost() {
        assertThatThrownBy(() -> service.update(13L, request("BH-13", "799", 5L)))
                .hasMessageContaining("Someone else changed this book");
        BookView v = service.update(13L, request("BH-13", "849", 0L));
        assertThat(v.price()).isEqualByComparingTo(new BigDecimal("849"));
    }

    @Test
    void orderedBooksAreRetiredOthersDeleted() {
        when(f.orderItems.existsByBookId(13L)).thenReturn(true);
        assertThat(service.remove(13L)).isFalse();
        assertThat(f.cleanCode.isActive()).isFalse();
        verify(f.books, never()).delete(any());

        assertThat(service.remove(20L)).isTrue();
        verify(f.books).delete(f.ikigai);
    }

    @Test
    void retiredBooksDisappearFromTheShop() {
        f.cleanCode.setActive(false);
        assertThatThrownBy(() -> service.get(13L)).hasMessage("Book not found");
    }
}
