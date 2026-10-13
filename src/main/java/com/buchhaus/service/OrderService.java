package com.buchhaus.service;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.core.Pricing;
import com.buchhaus.domain.Book;
import com.buchhaus.domain.CartItem;
import com.buchhaus.domain.CustomerOrder;
import com.buchhaus.domain.OrderItem;
import com.buchhaus.domain.User;
import com.buchhaus.repository.BookRepository;
import com.buchhaus.repository.CartItemRepository;
import com.buchhaus.repository.OrderRepository;
import com.buchhaus.repository.UserRepository;
import com.buchhaus.web.dto.CheckoutRequest;
import com.buchhaus.web.dto.OrderView;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout is one transaction: lock the books (SELECT ... FOR UPDATE, in id order), re-check
 * stock, take the copies, write the order with price snapshots and empty the cart. If anything
 * fails nothing is committed, so stock and orders can never disagree.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartItemRepository cartItems;
    private final BookRepository books;
    private final UserRepository users;
    private final Clock clock;

    public OrderService(OrderRepository orders, CartItemRepository cartItems, BookRepository books,
                        UserRepository users, Clock clock) {
        this.orders = orders;
        this.cartItems = cartItems;
        this.books = books;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public OrderView checkout(Long userId, CheckoutRequest shipping) {
        List<CartItem> cart = cartItems.findByUserIdOrderByIdAsc(userId);
        if (cart.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        Map<Long, Book> locked = books.lockAllById(cart.stream().map(c -> c.getBook().getId()).toList()).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        CustomerOrder order = new CustomerOrder(user, shipping.name().trim(), shipping.phone(),
                shipping.address().trim(), shipping.city().trim(), shipping.pincode());
        long subtotal = 0;
        for (CartItem line : cart) {
            Book book = locked.get(line.getBook().getId());
            if (book == null || !book.isActive()) {
                throw ApiException.conflict("\"" + line.getBook().getTitle() + "\" is no longer sold - remove it from your cart");
            }
            if (book.getStock() < line.getQuantity()) {
                throw ApiException.conflict(book.getStock() == 0
                        ? "\"" + book.getTitle() + "\" just sold out"
                        : "Only " + book.getStock() + " copies of \"" + book.getTitle() + "\" left");
            }
            book.removeStock(line.getQuantity());
            order.addItem(book, line.getQuantity());
            subtotal += Pricing.lineTotal(book.getPricePaise(), line.getQuantity());
        }
        order.setTotals(subtotal, Pricing.shipping(subtotal));
        CustomerOrder saved = orders.save(order);
        cartItems.deleteAllForUser(userId);
        return OrderView.of(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderView> mine(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(OrderView::of).toList();
    }

    @Transactional(readOnly = true)
    public OrderView get(Long userId, Long orderId) {
        return OrderView.of(owned(userId, orderId));
    }

    /** Customers can cancel until the order ships; the copies go back on the shelf. */
    @Transactional
    public OrderView cancel(Long userId, Long orderId) {
        CustomerOrder order = owned(userId, orderId);
        if (!order.getStatus().customerCanCancel()) {
            throw ApiException.conflict("This order has already " + order.getStatus().name().toLowerCase()
                    + " and can't be cancelled");
        }
        return OrderView.of(transition(order, OrderStatus.CANCELLED));
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<OrderView> all(OrderStatus status) {
        List<CustomerOrder> found = status == null ? orders.findAllByOrderByCreatedAtDescIdDesc()
                : orders.findByStatusOrderByCreatedAtDescIdDesc(status);
        return found.stream().map(OrderView::of).toList();
    }

    @Transactional
    public OrderView updateStatus(Long orderId, OrderStatus target) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!order.getStatus().canMoveTo(target)) {
            throw ApiException.conflict("Can't move an order from " + order.getStatus() + " to " + target);
        }
        return OrderView.of(transition(order, target));
    }

    private CustomerOrder transition(CustomerOrder order, OrderStatus target) {
        if (target == OrderStatus.CANCELLED) {
            List<Long> ids = order.getItems().stream().map(i -> i.getBook().getId()).toList();
            Map<Long, Book> locked = books.lockAllById(ids).stream()
                    .collect(Collectors.toMap(Book::getId, Function.identity()));
            for (OrderItem item : order.getItems()) {
                locked.get(item.getBook().getId()).restock(item.getQuantity());
            }
        }
        order.moveTo(target, clock.instant());
        return orders.save(order);
    }

    private CustomerOrder owned(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
    }
}
