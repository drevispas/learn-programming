package com.ecommerce.domain.order;

public record CustomerId(long value) {
    public CustomerId {
        if (value <= 0) {
            throw new IllegalArgumentException("CustomerId must be positive");
        }
    }
}
