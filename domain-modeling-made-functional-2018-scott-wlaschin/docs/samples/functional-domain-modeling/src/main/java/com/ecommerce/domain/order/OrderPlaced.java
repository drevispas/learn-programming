package com.ecommerce.domain.order;

import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 주문 완료 이벤트
 */
public record OrderPlaced(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    LocalDateTime occurredAt
) {
    public OrderPlaced {
        if (orderId == null) throw new IllegalArgumentException("OrderId required");
        if (customerId == null) throw new IllegalArgumentException("CustomerId required");
        if (totalAmount == null) throw new IllegalArgumentException("TotalAmount required");
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }

    public static OrderPlaced from(Order order) {
        return new OrderPlaced(
            order.id(),
            order.customerId(),
            order.totalAmount(),
            LocalDateTime.now()
        );
    }
}
