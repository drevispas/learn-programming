package com.ecommerce.domain.payment;

import java.util.UUID;

/**
 * 거래 ID Value Object
 */
public record TransactionId(String value) {
    public TransactionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TransactionId must not be blank");
        }
    }

    public static TransactionId generate() {
        return new TransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
