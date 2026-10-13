package com.buchhaus.web.dto;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.repository.TopSeller;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminStats(BigDecimal revenue, long orders, Map<OrderStatus, Long> ordersByStatus, long activeTitles,
                         List<BookView> lowStock, List<TopSeller> topSellers) {
}
