package com.ecommerce.domain.order;

import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 환불 정보
 */
public record RefundInfo(
    String originalTransactionId,
    Money refundAmount,
    LocalDateTime refundedAt
) {
    public RefundInfo {
        if (originalTransactionId == null || originalTransactionId.isBlank()) {
            throw new IllegalArgumentException("OriginalTransactionId required");
        }
        if (refundAmount == null) throw new IllegalArgumentException("RefundAmount required");
        if (refundedAt == null) refundedAt = LocalDateTime.now();
    }
}
