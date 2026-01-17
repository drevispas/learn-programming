package com.ecommerce.domain.product;

/**
 * 판매자 식별자 Value Object (Chapter 2)
 *
 * 불변식: 양수만 허용
 */
public record SellerId(long value) {
    public SellerId {
        if (value <= 0) {
            throw new IllegalArgumentException("SellerId must be positive");
        }
    }
}
