package com.buchhaus.service;

import com.buchhaus.core.Pricing;
import com.buchhaus.domain.Book;
import com.buchhaus.repository.BookRepository;
import com.buchhaus.repository.OrderItemRepository;
import com.buchhaus.web.dto.BookRequest;
import com.buchhaus.web.dto.BookView;
import com.buchhaus.web.dto.PageView;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalogue browsing for everyone, and title management for admins. */
@Service
public class BookService {

    public static final int MAX_PAGE_SIZE = 48;
    private static final Map<String, Sort> SORTS = Map.of(
            "relevance", Sort.by("title"),
            "title", Sort.by("title"),
            "price_asc", Sort.by(Sort.Direction.ASC, "pricePaise"),
            "price_desc", Sort.by(Sort.Direction.DESC, "pricePaise"),
            "newest", Sort.by(Sort.Direction.DESC, "publishedYear"));

    private final BookRepository books;
    private final OrderItemRepository orderItems;

    public BookService(BookRepository books, OrderItemRepository orderItems) {
        this.books = books;
        this.orderItems = orderItems;
    }

    @Transactional(readOnly = true)
    public PageView<BookView> search(String q, String genre, String sort, int page, int size) {
        Sort order = SORTS.getOrDefault(sort == null ? "relevance" : sort, SORTS.get("relevance"));
        PageRequest request = PageRequest.of(Math.max(0, page), Math.min(MAX_PAGE_SIZE, Math.max(1, size)),
                order);
        return PageView.of(books.search(blankToNull(q), blankToNull(genre), request), BookView::of);
    }

    @Transactional(readOnly = true)
    public BookView get(Long id) {
        return BookView.of(books.findByIdAndActiveTrue(id).orElseThrow(() -> ApiException.notFound("Book")));
    }

    @Transactional(readOnly = true)
    public List<String> genres() {
        return books.findGenres();
    }

    // ---------- admin ----------

    @Transactional
    public BookView create(BookRequest r) {
        if (books.existsBySkuIgnoreCase(r.sku())) {
            throw ApiException.conflict("SKU " + r.sku() + " is already used");
        }
        Book b = new Book(r.sku(), r.title().trim(), r.author().trim(), r.genre().trim(), toPaise(r), r.stock());
        apply(b, r);
        return BookView.of(books.save(b));
    }

    @Transactional
    public BookView update(Long id, BookRequest r) {
        Book b = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        if (r.version() != null && r.version() != b.getVersion()) {
            throw ApiException.conflict("Someone else changed this book - reload and try again");
        }
        if (!b.getSku().equalsIgnoreCase(r.sku()) && books.existsBySkuIgnoreCase(r.sku())) {
            throw ApiException.conflict("SKU " + r.sku() + " is already used");
        }
        b.setSku(r.sku());
        b.setTitle(r.title().trim());
        b.setAuthor(r.author().trim());
        b.setGenre(r.genre().trim());
        b.setPricePaise(toPaise(r));
        b.setStock(r.stock());
        apply(b, r);
        b.setActive(true);
        return BookView.of(books.saveAndFlush(b));   // flush so the response carries the new @Version
    }

    /**
     * Books that appear in past orders are retired (hidden from the shop, kept for order history);
     * books never ordered are deleted outright.
     */
    @Transactional
    public boolean remove(Long id) {
        Book b = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        if (orderItems.existsByBookId(id)) {
            b.setActive(false);
            books.save(b);
            return false;
        }
        books.delete(b);
        return true;
    }

    @Transactional(readOnly = true)
    public List<BookView> all() {
        return books.findAll(Sort.by("title")).stream().map(BookView::of).toList();
    }

    private static void apply(Book b, BookRequest r) {
        b.setDescription(blankToNull(r.description()));
        b.setPublishedYear(r.publishedYear());
        b.setPages(r.pages());
    }

    private static long toPaise(BookRequest r) {
        try {
            return Pricing.paise(r.price());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
