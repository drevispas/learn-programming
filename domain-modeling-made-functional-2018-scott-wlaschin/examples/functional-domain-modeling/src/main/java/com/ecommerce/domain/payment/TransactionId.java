package com.ecommerce.domain.payment;

import java.util.UUID;

/**
 * 결제 거래 식별자 Value Object (Chapter 2)
 *
 * 외부 결제 시스템에서 받은 거래 ID를 래핑한다.
 * 환불 시 이 ID로 원 거래를 참조한다.
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
