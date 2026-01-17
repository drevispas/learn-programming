package com.ecommerce.domain.order;

/**
 * 고객 식별자 Value Object (Chapter 2)
 *
 * <h2>목적 (Purpose)</h2>
 * long 대신 타입으로 의미 부여. findOrder(long, long) 같은 혼동 방지.
 *
 * <h2>불변식 (Invariant)</h2>
 * 값은 양수여야 한다 (0 이하 불가).
 */
public record CustomerId(long value) {
    public CustomerId {
        if (value <= 0) {
            throw new IllegalArgumentException("CustomerId must be positive");
        }
    }
}
