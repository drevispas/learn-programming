package com.ecommerce.domain.order;

import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 환불 정보 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: 상태별 데이터</h2>
 * 결제 취소 시에만 존재하는 정보. OrderStatus.Cancelled에 포함됨.
 * 미결제 취소 시에는 RefundInfo가 null (환불할 대상이 없으므로).
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
