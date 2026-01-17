package com.ecommerce.sample;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// =====================================================
// Sum Types (OR Types) - sealed interface를 통한 유한 타입 정의
// =====================================================

// === 결제 수단 (Payment Method) ===
// 결제 수단은 정확히 4가지 중 하나
sealed interface PaymentMethod
    permits PaymentMethod.CreditCard, PaymentMethod.BankTransfer,
            PaymentMethod.Points, PaymentMethod.SimplePay {

    record CreditCard(
        String cardNumber,
        String expiryDate,
        String cvc
    ) implements PaymentMethod {
        public CreditCard {
            if (cardNumber == null || cardNumber.isBlank())
                throw new IllegalArgumentException("Card number required");
            if (expiryDate == null || expiryDate.isBlank())
                throw new IllegalArgumentException("Expiry date required");
            if (cvc == null || cvc.length() != 3)
                throw new IllegalArgumentException("CVC must be 3 digits");
        }
    }

    record BankTransfer(
        String bankCode,
        String accountNumber,
        String holderName
    ) implements PaymentMethod {
        public BankTransfer {
            if (bankCode == null || bankCode.isBlank())
                throw new IllegalArgumentException("Bank code required");
            if (accountNumber == null || accountNumber.isBlank())
                throw new IllegalArgumentException("Account number required");
        }
    }

    record Points(int amount) implements PaymentMethod {
        public Points {
            if (amount < 0)
                throw new IllegalArgumentException("Points must be non-negative");
        }
    }

    record SimplePay(
        SimplePayProvider provider,
        String transactionToken
    ) implements PaymentMethod {
        public SimplePay {
            if (provider == null)
                throw new IllegalArgumentException("Provider required");
            if (transactionToken == null || transactionToken.isBlank())
                throw new IllegalArgumentException("Transaction token required");
        }
    }
}

enum SimplePayProvider { KAKAO, NAVER, TOSS }


// === 주문 상태 (Order Status) ===
// 각 상태마다 필요한 데이터가 다름 - 불가능한 상태 제거
sealed interface OrderStatus
    permits OrderStatus.Unpaid, OrderStatus.Paid,
            OrderStatus.Shipping, OrderStatus.Delivered, OrderStatus.Cancelled {

    record Unpaid(LocalDateTime paymentDeadline) implements OrderStatus {}

    record Paid(
        LocalDateTime paidAt,
        PaymentMethod paymentMethod,
        String transactionId
    ) implements OrderStatus {}

    record Shipping(
        LocalDateTime paidAt,
        String trackingNumber,
        LocalDateTime shippedAt
    ) implements OrderStatus {}

    record Delivered(
        LocalDateTime paidAt,
        String trackingNumber,
        LocalDateTime deliveredAt
    ) implements OrderStatus {}

    record Cancelled(
        LocalDateTime cancelledAt,
        CancelReason reason
    ) implements OrderStatus {}
}

enum CancelReason {
    CUSTOMER_REQUEST, OUT_OF_STOCK, PAYMENT_FAILED, FRAUD_SUSPECTED
}


// === 쿠폰 타입 (Coupon Type) ===
// 쿠폰은 정액 할인 OR 정률 할인 OR 무료 배송
sealed interface CouponType
    permits CouponType.FixedAmountDiscount, CouponType.PercentageDiscount,
            CouponType.FreeShipping {

    Money calculateDiscount(Money originalPrice);

    record FixedAmountDiscount(Money discountAmount) implements CouponType {
        public FixedAmountDiscount {
            if (discountAmount == null || discountAmount.isNegativeOrZero()) {
                throw new IllegalArgumentException("Discount amount must be positive");
            }
        }

        @Override
        public Money calculateDiscount(Money originalPrice) {
            return originalPrice.isLessThan(discountAmount)
                ? originalPrice
                : discountAmount;
        }
    }

    record PercentageDiscount(int rate) implements CouponType {
        public PercentageDiscount {
            if (rate < 1 || rate > 100) {
                throw new IllegalArgumentException("Rate must be between 1 and 100");
            }
        }

        @Override
        public Money calculateDiscount(Money originalPrice) {
            return originalPrice.multiply(BigDecimal.valueOf(rate)).divide(100);
        }
    }

    record FreeShipping(Money shippingFee) implements CouponType {
        @Override
        public Money calculateDiscount(Money originalPrice) {
            return shippingFee;
        }
    }
}


// === 결제 결과 (Payment Result) ===
sealed interface PaymentResult
    permits PaymentResult.PaymentSuccess, PaymentResult.PaymentFailure {

    record PaymentSuccess(String transactionId, Money amount) implements PaymentResult {}
    record PaymentFailure(PaymentError error) implements PaymentResult {}
}


// === 결제 에러 (Payment Error) ===
sealed interface PaymentError
    permits PaymentError.InsufficientFunds, PaymentError.CardExpired,
            PaymentError.SystemError {

    record InsufficientFunds(Money required) implements PaymentError {}
    record CardExpired(String expiryDate) implements PaymentError {}
    record SystemError(String message) implements PaymentError {}
}
