package com.ecommerce.domain.member;

/**
 * 회원 포인트 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: 도메인 연산 포함</h2>
 * 단순 래퍼가 아닌, 포인트 관련 비즈니스 로직(적립, 사용)을 포함한다.
 *
 * <h2>불변식 (Invariant)</h2>
 * 음수 불가 (잔액이 부족하면 subtract에서 예외 발생)
 */
public record Points(int value) {
    public Points {
        if (value < 0) {
            throw new IllegalArgumentException("Points must be >= 0");
        }
    }

    public static final Points ZERO = new Points(0);

    public Points add(int amount) {
        return new Points(value + amount);
    }

    public Points subtract(int amount) {
        if (value < amount) {
            throw new IllegalArgumentException("Insufficient points: has=" + value + ", required=" + amount);
        }
        return new Points(value - amount);
    }

    public boolean hasEnough(int required) {
        return value >= required;
    }
}
