package com.ecommerce.domain.product;

import java.math.BigDecimal;

/**
 * 수수료율 Value Object
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
