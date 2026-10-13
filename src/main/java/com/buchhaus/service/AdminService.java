package com.buchhaus.service;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.core.Pricing;
import com.buchhaus.repository.BookRepository;
import com.buchhaus.repository.OrderRepository;
import com.buchhaus.web.dto.AdminStats;
import com.buchhaus.web.dto.BookView;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    public static final int LOW_STOCK = 5;

    private final OrderRepository orders;
    private final BookRepository books;

    public AdminService(OrderRepository orders, BookRepository books) {
        this.orders = orders;
        this.books = books;
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        long total = 0;
        for (OrderStatus s : OrderStatus.values()) {
            long n = orders.countByStatus(s);
            byStatus.put(s, n);
            total += n;
        }
        return new AdminStats(Pricing.rupees(orders.revenueExcluding(OrderStatus.CANCELLED)), total, byStatus,
                books.countByActiveTrue(),
                books.findTop5ByActiveTrueAndStockLessThanOrderByStockAsc(LOW_STOCK).stream().map(BookView::of).toList(),
                orders.topSellers(OrderStatus.CANCELLED, PageRequest.of(0, 5)));
    }
}
