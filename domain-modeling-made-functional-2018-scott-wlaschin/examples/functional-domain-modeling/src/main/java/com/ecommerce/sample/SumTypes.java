package com.ecommerce.sample;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sum Types (OR Types) 패턴 예제 (Chapter 4)
 *
 * <h2>목적 (Purpose)</h2>
 * "A 이거나 B 이거나 C" 같은 유한 개의 선택지를 타입으로 표현한다.
 * Java의 sealed interface + permits로 구현하며, "가능한 경우"를 컴파일 타임에 고정한다.
 *
 * <h2>핵심 개념 (Key Concept): Product Type vs Sum Type</h2>
 * <pre>
 * Product Type (AND): record Address(String street, String city, String zip)
 *   → 모든 필드를 동시에 가짐. 가능한 조합 = street 경우의 수 × city × zip
 *
 * Sum Type (OR): sealed interface PaymentMethod permits CreditCard, BankTransfer, Points
 *   → 셋 중 정확히 하나만 선택. 가능한 경우 = CreditCard + BankTransfer + Points
 * </pre>
 *
 * <h2>왜 Sum Type이 필요한가?</h2>
 * <pre>{@code
 * // Before: 잘못된 조합이 가능
 * record Payment(String cardNumber, String bankAccount, Integer points) {
 *     // cardNumber도 있고 bankAccount도 있는 상태? 어떻게 처리?
 * }
 *
 * // After: 정확히 하나의 결제 수단만 가능
 * sealed interface PaymentMethod permits CreditCard, BankTransfer, Points {}
 * }</pre>
 *
 * <h2>얻어갈 것 (Takeaway): Exhaustive Pattern Matching</h2>
 * <pre>{@code
 * String description = switch (paymentMethod) {
 *     case CreditCard c -> "카드 " + c.cardNumber();
 *     case BankTransfer b -> "계좌 " + b.accountNumber();
 *     case Points p -> "포인트 " + p.amount();
 *     case SimplePay s -> s.provider() + " 페이";
 *     // 컴파일러가 모든 케이스 처리를 강제 - 새 결제 수단 추가 시 컴파일 에러
 * };
 * }</pre>
 *
 * @see com.ecommerce.shared.Result Sum Type의 대표 예제 (Success | Failure)
 */

// =====================================================
// Sum Types (OR Types) - sealed interface를 통한 유한 타입 정의
// =====================================================

// ============================================================================
// 결제 수단 (Payment Method)
// ============================================================================

/**
 * 결제 수단 - 4가지 중 정확히 하나
 *
 * 각 variant가 필요한 데이터만 정의하여 "유효하지 않은 상태" 자체를 불가능하게 만든다.
 * 예: 카드 결제에는 cardNumber가 필요하지만, 포인트 결제에는 불필요.
 */
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


// ============================================================================
// 주문 상태 (Order Status) - 상태별 다른 데이터
// ============================================================================

/**
 * 주문 상태 - "Make Illegal States Unrepresentable" 패턴의 대표 예제
 *
 * <h3>핵심: 각 상태마다 필요한 데이터가 다르다</h3>
 * <pre>
 * Unpaid   → paymentDeadline만 필요
 * Paid     → paidAt, paymentMethod, transactionId 필요
 * Shipping → 위 + trackingNumber, shippedAt 필요
 * ...
 * </pre>
 *
 * <h3>Before (잘못된 설계)</h3>
 * <pre>{@code
 * record Order(
 *     String status,              // "UNPAID", "PAID", "SHIPPING"...
 *     LocalDateTime paymentDeadline,  // null if paid
 *     LocalDateTime paidAt,           // null if unpaid
 *     String trackingNumber,          // null if not shipping
 *     ...
 * )
 * // 문제: paidAt이 null인데 status가 "PAID"인 불일치 상태 가능
 * }</pre>
 *
 * <h3>After (Sum Type으로 불일치 제거)</h3>
 * <pre>{@code
 * switch (status) {
 *     case Paid p -> // p.paidAt()은 항상 존재 (non-null 보장)
 *     case Unpaid u -> // u.paymentDeadline()만 존재
 * }
 * }</pre>
 */
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
