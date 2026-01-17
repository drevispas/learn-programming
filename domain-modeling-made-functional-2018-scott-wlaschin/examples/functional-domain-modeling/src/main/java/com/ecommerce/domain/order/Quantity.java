package com.ecommerce.domain.order;

public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
    }
}
