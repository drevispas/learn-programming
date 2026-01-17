package com.ecommerce.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * 주문 레포지토리 인터페이스
 * 도메인 레이어에 정의되어 인프라 레이어에서 구현
 */
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId id);
}
