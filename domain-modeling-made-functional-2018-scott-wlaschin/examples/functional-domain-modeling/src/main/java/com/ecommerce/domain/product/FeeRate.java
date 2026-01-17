package com.ecommerce.domain.product;

import java.math.BigDecimal;

/**
 * 수수료율 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: 도메인 제약을 타입으로 표현</h2>
 * 0~1 사이의 소수값 (0% ~ 100%).
 * 팩토리 메서드 FeeRate.of(10)으로 10% 수수료 생성.
 *
 * <h2>불변식 (Invariant)</h2>
 * 0.0 이상 1.0 이하
 */
public record FeeRate(BigDecimal value) {
    public FeeRate {
        if (value == null) {
            throw new IllegalArgumentException("FeeRate value required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("FeeRate must be between 0 and 1");
        }
    }

    public static FeeRate of(int percentage) {
        return new FeeRate(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100)));
    }

    public int asPercentage() {
        return value.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
