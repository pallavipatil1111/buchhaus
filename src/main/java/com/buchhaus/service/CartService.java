package com.buchhaus.service;

import com.buchhaus.core.Pricing;
import com.buchhaus.domain.Book;
import com.buchhaus.domain.CartItem;
import com.buchhaus.domain.User;
import com.buchhaus.repository.BookRepository;
import com.buchhaus.repository.CartItemRepository;
import com.buchhaus.repository.UserRepository;
import com.buchhaus.web.dto.BookView;
import com.buchhaus.web.dto.CartLine;
import com.buchhaus.web.dto.CartView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shopping cart is stored server-side per user, so it survives log-outs and device changes. */
@Service
public class CartService {

    private final CartItemRepository cartItems;
    private final BookRepository books;
    private final UserRepository users;

    public CartService(CartItemRepository cartItems, BookRepository books, UserRepository users) {
        this.cartItems = cartItems;
        this.books = books;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CartView view(Long userId) {
        return summarise(cartItems.findByUserIdOrderByIdAsc(userId));
    }

    /** Sets the quantity of a book in the cart; 0 removes it. Quantity can't exceed stock. */
    @Transactional
    public CartView setQuantity(Long userId, Long bookId, int quantity) {
        if (quantity < 0 || quantity > Pricing.MAX_QUANTITY_PER_BOOK) {
            throw ApiException.badRequest("Quantity must be between 0 and " + Pricing.MAX_QUANTITY_PER_BOOK);
        }
        var existing = cartItems.findByUserIdAndBookId(userId, bookId);
        if (quantity == 0) {
            existing.ifPresent(cartItems::delete);
            return view(userId);
        }
        Book book = books.findByIdAndActiveTrue(bookId).orElseThrow(() -> ApiException.notFound("Book"));
        if (quantity > book.getStock()) {
            throw ApiException.conflict(book.getStock() == 0 ? "\"" + book.getTitle() + "\" is out of stock"
                    : "Only " + book.getStock() + " copies of \"" + book.getTitle() + "\" left");
        }
        if (existing.isPresent()) {
            existing.get().setQuantity(quantity);
            cartItems.save(existing.get());
        } else {
            User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
            cartItems.save(new CartItem(user, book, quantity));
        }
        return view(userId);
    }

    @Transactional
    public CartView clear(Long userId) {
        cartItems.deleteAllForUser(userId);
        return view(userId);
    }

    static CartView summarise(List<CartItem> items) {
        long subtotal = 0;
        int count = 0;
        boolean allAvailable = !items.isEmpty();
        List<CartLine> lines = new java.util.ArrayList<>();
        for (CartItem item : items) {
            Book b = item.getBook();
            boolean available = b.isActive() && b.getStock() >= item.getQuantity();
            allAvailable &= available;
            long line = Pricing.lineTotal(b.getPricePaise(), item.getQuantity());
            subtotal += line;
            count += item.getQuantity();
            lines.add(new CartLine(BookView.of(b), item.getQuantity(), Pricing.rupees(line), available));
        }
        long shipping = Pricing.shipping(subtotal);
        long gap = subtotal == 0 ? Pricing.FREE_SHIPPING_FROM : Math.max(0, Pricing.FREE_SHIPPING_FROM - subtotal);
        return new CartView(lines, count, Pricing.rupees(subtotal), Pricing.rupees(shipping),
                Pricing.rupees(subtotal + shipping), Pricing.rupees(gap), allAvailable);
    }
}
