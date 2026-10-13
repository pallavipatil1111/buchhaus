package com.buchhaus.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.buchhaus.domain.Book;
import com.buchhaus.domain.CartItem;
import com.buchhaus.domain.Role;
import com.buchhaus.domain.User;
import com.buchhaus.repository.BookRepository;
import com.buchhaus.repository.CartItemRepository;
import com.buchhaus.repository.OrderItemRepository;
import com.buchhaus.repository.OrderRepository;
import com.buchhaus.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** In-memory catalogue + cart wired into mocked repositories. */
final class Fixture {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);

    final BookRepository books = mock(BookRepository.class);
    final CartItemRepository cartItems = mock(CartItemRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final User reader = user(1L, "reader@buchhaus.local");
    final List<CartItem> cart = new ArrayList<>();
    final Book cleanCode = book(13L, "Clean Code", 799_00, 12);
    final Book ikigai = book(20L, "Ikigai", 350_00, 2);
    final Book soldOut = book(24L, "Five Point Someone", 199_00, 0);

    Fixture() {
        when(users.findById(1L)).thenReturn(Optional.of(reader));
        for (Book b : List.of(cleanCode, ikigai, soldOut)) {
            when(books.findById(b.getId())).thenReturn(Optional.of(b));
            when(books.findByIdAndActiveTrue(b.getId())).thenAnswer(inv -> b.isActive() ? Optional.of(b) : Optional.empty());
        }
        when(books.lockAllById(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return List.of(cleanCode, ikigai, soldOut).stream().filter(b -> ids.contains(b.getId())).toList();
        });
        when(cartItems.findByUserIdOrderByIdAsc(1L)).thenAnswer(inv -> List.copyOf(cart));
        when(cartItems.findByUserIdAndBookId(any(), any())).thenAnswer(inv -> cart.stream()
                .filter(c -> c.getBook().getId().equals(inv.getArgument(1))).findFirst());
        when(cartItems.save(any())).thenAnswer(inv -> {
            CartItem c = inv.getArgument(0);
            if (!cart.contains(c)) {
                cart.add(c);
            }
            return c;
        });
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(books.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(books.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    void inCart(Book book, int qty) {
        cart.add(new CartItem(reader, book, qty));
    }

    static User user(Long id, String email) {
        User u = new User("Reader", email, "hash", Role.CUSTOMER);
        u.setId(id);
        return u;
    }

    static Book book(Long id, String title, long paise, int stock) {
        Book b = new Book("BH-" + id, title, "Author", "Technology", paise, stock);
        b.setId(id);
        return b;
    }
}
