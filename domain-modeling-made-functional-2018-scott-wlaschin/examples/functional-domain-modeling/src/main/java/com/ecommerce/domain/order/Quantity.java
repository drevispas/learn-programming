package com.ecommerce.domain.order;

/**
 * 수량 Value Object (Chapter 2)
 *
 * <h2>불변식 (Invariant)</h2>
 * 최소 1개 이상 (0개 주문은 무의미).
 */
public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
    }
}
