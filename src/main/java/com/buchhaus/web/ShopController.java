package com.buchhaus.web;

import com.buchhaus.security.AuthUser;
import com.buchhaus.service.BookService;
import com.buchhaus.service.CartService;
import com.buchhaus.service.OrderService;
import com.buchhaus.web.dto.BookView;
import com.buchhaus.web.dto.CartView;
import com.buchhaus.web.dto.CheckoutRequest;
import com.buchhaus.web.dto.OrderView;
import com.buchhaus.web.dto.PageView;
import com.buchhaus.web.dto.QuantityRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Customer-facing API: catalogue (public), cart and orders (signed in). */
@RestController
public class ShopController {

    private final BookService books;
    private final CartService cart;
    private final OrderService orders;

    public ShopController(BookService books, CartService cart, OrderService orders) {
        this.books = books;
        this.cart = cart;
        this.orders = orders;
    }

    @GetMapping("/api/books")
    public PageView<BookView> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(defaultValue = "relevance") String sort,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size) {
        return books.search(q, genre, sort, page, size);
    }

    @GetMapping("/api/books/{id}")
    public BookView book(@PathVariable Long id) {
        return books.get(id);
    }

    @GetMapping("/api/genres")
    public List<String> genres() {
        return books.genres();
    }

    @GetMapping("/api/cart")
    public CartView cart(@AuthenticationPrincipal AuthUser me) {
        return cart.view(me.id());
    }

    @PutMapping("/api/cart/items/{bookId}")
    public CartView setQuantity(@AuthenticationPrincipal AuthUser me, @PathVariable Long bookId,
                                @Valid @RequestBody QuantityRequest request) {
        return cart.setQuantity(me.id(), bookId, request.quantity());
    }

    @DeleteMapping("/api/cart/items/{bookId}")
    public CartView remove(@AuthenticationPrincipal AuthUser me, @PathVariable Long bookId) {
        return cart.setQuantity(me.id(), bookId, 0);
    }

    @DeleteMapping("/api/cart")
    public CartView clear(@AuthenticationPrincipal AuthUser me) {
        return cart.clear(me.id());
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CheckoutRequest request) {
        return orders.checkout(me.id(), request);
    }

    @GetMapping("/api/orders")
    public List<OrderView> myOrders(@AuthenticationPrincipal AuthUser me) {
        return orders.mine(me.id());
    }

    @GetMapping("/api/orders/{id}")
    public OrderView order(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.get(me.id(), id);
    }

    @PostMapping("/api/orders/{id}/cancel")
    public OrderView cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.cancel(me.id(), id);
    }
}
