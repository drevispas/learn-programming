package com.ecommerce.domain.order;

import com.ecommerce.domain.payment.PaymentMethod;

import java.time.LocalDateTime;

/**
 * 주문 상태 - 각 상태마다 필요한 데이터가 다름
 * sealed interface로 타입을 제한하여 exhaustive pattern matching 강제
 */
public sealed interface OrderStatus
    permits OrderStatus.Unpaid, OrderStatus.Paid, OrderStatus.Shipping,
            OrderStatus.Delivered, OrderStatus.Cancelled {

    /**
     * 미결제 상태
     */
    record Unpaid(LocalDateTime createdAt, LocalDateTime paymentDeadline) implements OrderStatus {
        public Unpaid {
            if (createdAt == null) createdAt = LocalDateTime.now();
            if (paymentDeadline == null) {
                throw new IllegalArgumentException("Payment deadline required");
            }
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(paymentDeadline);
        }
    }

    /**
     * 결제 완료 상태
     */
    record Paid(
        LocalDateTime paidAt,
        PaymentMethod paymentMethod,
        String transactionId
    ) implements OrderStatus {
        public Paid {
            if (paidAt == null) paidAt = LocalDateTime.now();
            if (paymentMethod == null) throw new IllegalArgumentException("PaymentMethod required");
            if (transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("TransactionId required");
            }
        }
    }

    /**
     * 배송 중 상태
     */
    record Shipping(
        LocalDateTime paidAt,
        String trackingNumber,
        LocalDateTime shippedAt,
        String carrier
    ) implements OrderStatus {
        public Shipping {
            if (paidAt == null) throw new IllegalArgumentException("PaidAt required");
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new IllegalArgumentException("TrackingNumber required");
            }
            if (shippedAt == null) shippedAt = LocalDateTime.now();
        }

        public Shipping(LocalDateTime paidAt, String trackingNumber, LocalDateTime shippedAt) {
            this(paidAt, trackingNumber, shippedAt, null);
        }
    }

    /**
     * 배송 완료 상태
     */
    record Delivered(
        LocalDateTime paidAt,
        String trackingNumber,
        LocalDateTime deliveredAt
    ) implements OrderStatus {
        public Delivered {
            if (paidAt == null) throw new IllegalArgumentException("PaidAt required");
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new IllegalArgumentException("TrackingNumber required");
            }
            if (deliveredAt == null) deliveredAt = LocalDateTime.now();
        }
    }

    /**
     * 취소됨 상태
     */
    record Cancelled(
        LocalDateTime cancelledAt,
        CancelReason reason,
        RefundInfo refundInfo
    ) implements OrderStatus {
        public Cancelled {
            if (cancelledAt == null) cancelledAt = LocalDateTime.now();
            if (reason == null) throw new IllegalArgumentException("CancelReason required");
        }

        public Cancelled(LocalDateTime cancelledAt, CancelReason reason) {
            this(cancelledAt, reason, null);
        }

        public boolean hasRefund() {
            return refundInfo != null;
        }
    }
}
