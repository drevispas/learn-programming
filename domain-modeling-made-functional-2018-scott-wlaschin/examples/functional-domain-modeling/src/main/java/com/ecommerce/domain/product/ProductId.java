package com.ecommerce.domain.product;

/**
 * 상품 식별자 Value Object (Chapter 2)
 *
 * 불변식: null이거나 공백일 수 없다.
 */
public record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId must not be blank");
        }
    }
}
