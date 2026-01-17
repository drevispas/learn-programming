package com.ecommerce.domain.product;

/**
 * 상품명 Value Object (Chapter 2)
 *
 * 불변식: 공백 불가, 200자 이하
 */
public record ProductName(String value) {
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductName must not be blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("ProductName must be <= 200 characters");
        }
    }
}
