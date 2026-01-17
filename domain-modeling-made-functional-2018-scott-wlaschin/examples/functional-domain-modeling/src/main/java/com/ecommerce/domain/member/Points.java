package com.ecommerce.domain.member;

/**
 * 회원 포인트 Value Object
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
