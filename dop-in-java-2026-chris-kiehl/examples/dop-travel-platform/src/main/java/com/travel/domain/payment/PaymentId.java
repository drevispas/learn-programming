package com.travel.domain.payment;

import java.util.UUID;

/**
 * 결제 식별자 - 강타입 ID
 *
 * @param value UUID 값
 */
public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId는 null일 수 없습니다");
        }
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId from(String value) {
        try {
            return new PaymentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 PaymentId 형식: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
