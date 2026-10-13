package com.buchhaus.web.dto;

import com.buchhaus.core.Pricing;
import com.buchhaus.domain.Book;
import java.math.BigDecimal;

public record BookView(Long id, String sku, String title, String author, String genre, String description,
                       BigDecimal price, int stock, Integer publishedYear, Integer pages, boolean active,
                       long version) {

    public static BookView of(Book b) {
        return new BookView(b.getId(), b.getSku(), b.getTitle(), b.getAuthor(), b.getGenre(), b.getDescription(),
                Pricing.rupees(b.getPricePaise()), b.getStock(), b.getPublishedYear(), b.getPages(), b.isActive(),
                b.getVersion());
    }
}
