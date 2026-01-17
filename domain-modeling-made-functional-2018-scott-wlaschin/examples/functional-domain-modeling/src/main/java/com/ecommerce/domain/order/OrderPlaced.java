package com.ecommerce.domain.order;

import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 주문 완료 이벤트 - 워크플로우의 출력 (Chapter 9)
 *
 * <h2>핵심 개념: Domain Event</h2>
 * 워크플로우 완료 시 발생하는 이벤트. 과거형 이름(OrderPlaced, not PlaceOrder) 사용.
 * 다른 Bounded Context가 이 이벤트를 구독하여 후속 처리 수행.
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 주문 생성 후 이벤트 발행
 * OrderPlaced event = OrderPlaced.from(savedOrder);
 * eventPublisher.publish(event);
 *
 * // 다른 Context에서 이벤트 처리
 * // - 재고 시스템: 재고 차감 확정
 * // - 알림 시스템: 주문 확인 메일 발송
 * // - 정산 시스템: 정산 예정 금액 기록
 * }</pre>
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
