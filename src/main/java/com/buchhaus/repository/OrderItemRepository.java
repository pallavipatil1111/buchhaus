package com.buchhaus.repository;

import com.buchhaus.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByBookId(Long bookId);
}
