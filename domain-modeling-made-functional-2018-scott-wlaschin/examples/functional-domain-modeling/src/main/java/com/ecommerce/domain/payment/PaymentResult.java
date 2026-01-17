package com.ecommerce.domain.payment;

import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 결제 결과 - 성공 또는 실패
 * sealed interface로 타입을 제한하여 exhaustive pattern matching 강제
 */
public sealed interface PaymentResult
    permits PaymentResult.Success, PaymentResult.Failure {

    /**
     * 결제 성공
     */
    record Success(
        String transactionId,
        Money amount,
        PaymentMethod paymentMethod,
        LocalDateTime processedAt
    ) implements PaymentResult {
        public Success {
            if (transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("TransactionId required");
            }
            if (amount == null) throw new IllegalArgumentException("Amount required");
            if (paymentMethod == null) throw new IllegalArgumentException("PaymentMethod required");
            if (processedAt == null) processedAt = LocalDateTime.now();
        }

        public Success(String transactionId, Money amount, PaymentMethod paymentMethod) {
            this(transactionId, amount, paymentMethod, LocalDateTime.now());
        }
    }

    /**
     * 결제 실패
     */
    record Failure(
        PaymentError error,
        LocalDateTime failedAt
    ) implements PaymentResult {
        public Failure {
            if (error == null) throw new IllegalArgumentException("PaymentError required");
            if (failedAt == null) failedAt = LocalDateTime.now();
        }

        public Failure(PaymentError error) {
            this(error, LocalDateTime.now());
        }
    }
}
