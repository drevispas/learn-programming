package com.ecommerce.domain.product;

/**
 * 판매자 ID Value Object
 */
public record SellerId(long value) {
    public SellerId {
        if (value <= 0) {
            throw new IllegalArgumentException("SellerId must be positive");
        }
    }
}
