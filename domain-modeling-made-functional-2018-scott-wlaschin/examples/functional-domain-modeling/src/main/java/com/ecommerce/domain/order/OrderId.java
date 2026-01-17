package com.ecommerce.domain.order;

/**
 * 주문 식별자 Value Object (Chapter 2)
 *
 * <h2>목적 (Purpose)</h2>
 * String 대신 도메인 개념을 명시적으로 표현한다.
 *
 * <h2>불변식 (Invariant)</h2>
 * 값은 null이거나 공백일 수 없다.
 *
 * <h2>왜 record인가</h2>
 * 불변성 자동 보장, equals/hashCode 자동 생성으로 값 동등성 지원
 */
public record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId must not be blank");
        }
    }

    public static OrderId generate() {
        return new OrderId("ORD-" + System.currentTimeMillis());
    }
}
