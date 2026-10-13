package com.buchhaus.web;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.service.AdminService;
import com.buchhaus.service.BookService;
import com.buchhaus.service.OrderService;
import com.buchhaus.web.dto.AdminStats;
import com.buchhaus.web.dto.BookRequest;
import com.buchhaus.web.dto.BookView;
import com.buchhaus.web.dto.OrderView;
import com.buchhaus.web.dto.StatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin API. Every route here requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final BookService books;
    private final OrderService orders;

    public AdminController(AdminService admin, BookService books, OrderService orders) {
        this.admin = admin;
        this.books = books;
        this.orders = orders;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return admin.stats();
    }

    @GetMapping("/books")
    public List<BookView> books() {
        return books.all();
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookView create(@Valid @RequestBody BookRequest request) {
        return books.create(request);
    }

    @PutMapping("/books/{id}")
    public BookView update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return books.update(id, request);
    }

    /** Deletes the book, or retires it if it appears in past orders. */
    @DeleteMapping("/books/{id}")
    public Map<String, Object> remove(@PathVariable Long id) {
        boolean deleted = books.remove(id);
        return Map.of("id", id, "result", deleted ? "deleted" : "retired");
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@RequestParam(required = false) OrderStatus status) {
        return orders.all(status);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderView updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return orders.updateStatus(id, request.status());
    }
}
