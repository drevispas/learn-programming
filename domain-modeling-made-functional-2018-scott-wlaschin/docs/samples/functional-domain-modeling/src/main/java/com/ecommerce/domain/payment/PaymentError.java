package com.ecommerce.domain.payment;

import com.ecommerce.shared.types.Money;

/**
 * 결제 에러
 * sealed interface로 모든 에러 케이스 처리 강제
 */
public sealed interface PaymentError
    permits PaymentError.InsufficientFunds, PaymentError.CardExpired, PaymentError.CardDeclined,
            PaymentError.InvalidPaymentMethod, PaymentError.PaymentTimeout, PaymentError.SystemError {

    String message();
    String code();

    /**
     * 잔액/한도 부족
     */
    record InsufficientFunds(Money required, Money available) implements PaymentError {
        @Override
        public String message() {
            return "잔액이 부족합니다: 필요=" + required.amount() + ", 사용가능=" + available.amount();
        }

        @Override
        public String code() {
            return "INSUFFICIENT_FUNDS";
        }
    }

    /**
     * 카드 만료
     */
    record CardExpired(String expiryDate) implements PaymentError {
        @Override
        public String message() {
            return "카드가 만료되었습니다: " + expiryDate;
        }

        @Override
        public String code() {
            return "CARD_EXPIRED";
        }
    }

    /**
     * 카드 거절
     */
    record CardDeclined(String reason) implements PaymentError {
        @Override
        public String message() {
            return "카드가 거절되었습니다: " + reason;
        }

        @Override
        public String code() {
            return "CARD_DECLINED";
        }
    }

    /**
     * 유효하지 않은 결제 수단
     */
    record InvalidPaymentMethod(String reason) implements PaymentError {
        @Override
        public String message() {
            return "유효하지 않은 결제 수단입니다: " + reason;
        }

        @Override
        public String code() {
            return "INVALID_PAYMENT_METHOD";
        }
    }

    /**
     * 결제 타임아웃
     */
    record PaymentTimeout() implements PaymentError {
        @Override
        public String message() {
            return "결제 처리 시간이 초과되었습니다";
        }

        @Override
        public String code() {
            return "PAYMENT_TIMEOUT";
        }
    }

    /**
     * 시스템 에러
     */
    record SystemError(String internalMessage) implements PaymentError {
        @Override
        public String message() {
            return "결제 시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }

        @Override
        public String code() {
            return "SYSTEM_ERROR";
        }
    }
}
