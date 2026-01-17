package com.travel.domain.payment;

import com.travel.shared.types.Money;

/**
 * 결제 도메인 오류 - Error as Data
 *
 * <h2>목적 (Purpose)</h2>
 * 결제 관련 비즈니스 오류를 타입으로 명시적 표현
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Error as Data</h2>
 * <pre>
 * [Key Point] 예외 대신 값으로 오류 표현:
 * - 각 오류 케이스가 해당 오류에 필요한 정보만 보유
 * - 컴파일러가 모든 오류 케이스 처리 강제
 * </pre>
 */
public sealed interface PaymentError permits
        PaymentError.InsufficientFunds,
        PaymentError.CardDeclined,
        PaymentError.InvalidPaymentMethod,
        PaymentError.DuplicatePayment,
        PaymentError.PaymentExpired,
        PaymentError.RefundFailed,
        PaymentError.GatewayError,
        PaymentError.PaymentNotFound {

    String message();
    String code();

    /**
     * 잔액 부족
     */
    record InsufficientFunds(
            Money required,
            Money available
    ) implements PaymentError {
        @Override
        public String message() {
            return "잔액이 부족합니다. 필요: " + required + ", 가용: " + available;
        }

        @Override
        public String code() {
            return "INSUFFICIENT_FUNDS";
        }
    }

    /**
     * 카드 거절
     */
    record CardDeclined(
            String reason,
            String cardNumber
    ) implements PaymentError {
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
            return "결제 수단이 유효하지 않습니다: " + reason;
        }

        @Override
        public String code() {
            return "INVALID_PAYMENT_METHOD";
        }
    }

    /**
     * 중복 결제 (멱등성 키로 감지)
     *
     * <p>[Ch 7] 멱등성 관련 오류</p>
     *
     * @param idempotencyKey 중복된 멱등성 키
     * @param existingPaymentId 기존 결제 ID
     */
    record DuplicatePayment(
            IdempotencyKey idempotencyKey,
            PaymentId existingPaymentId
    ) implements PaymentError {
        @Override
        public String message() {
            return "중복 결제 요청입니다. 기존 결제 ID: " + existingPaymentId;
        }

        @Override
        public String code() {
            return "DUPLICATE_PAYMENT";
        }
    }

    /**
     * 결제 시간 만료
     */
    record PaymentExpired(PaymentId paymentId) implements PaymentError {
        @Override
        public String message() {
            return "결제 시간이 만료되었습니다: " + paymentId;
        }

        @Override
        public String code() {
            return "PAYMENT_EXPIRED";
        }
    }

    /**
     * 환불 실패
     */
    record RefundFailed(
            PaymentId paymentId,
            String reason
    ) implements PaymentError {
        @Override
        public String message() {
            return "환불에 실패했습니다: " + reason;
        }

        @Override
        public String code() {
            return "REFUND_FAILED";
        }
    }

    /**
     * 결제 게이트웨이 오류
     */
    record GatewayError(
            String gatewayName,
            String errorCode,
            String errorMessage
    ) implements PaymentError {
        @Override
        public String message() {
            return gatewayName + " 오류 [" + errorCode + "]: " + errorMessage;
        }

        @Override
        public String code() {
            return "GATEWAY_ERROR";
        }
    }

    /**
     * 결제 정보 없음
     */
    record PaymentNotFound(PaymentId paymentId) implements PaymentError {
        @Override
        public String message() {
            return "결제 정보를 찾을 수 없습니다: " + paymentId;
        }

        @Override
        public String code() {
            return "PAYMENT_NOT_FOUND";
        }
    }
}
