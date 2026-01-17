package com.ecommerce.domain.product;

/**
 * 재고 수량 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: 도메인 연산 포함</h2>
 * 단순 래퍼가 아닌, 재고 관련 비즈니스 로직(입고, 출고, 품절 확인)을 포함한다.
 *
 * <h2>불변식 (Invariant)</h2>
 * 음수 불가 (재고는 0 이상)
 */
public record StockQuantity(int value) {
    public StockQuantity {
        if (value < 0) {
            throw new IllegalArgumentException("StockQuantity must be >= 0");
        }
    }

    public static final StockQuantity ZERO = new StockQuantity(0);

    public StockQuantity add(int amount) {
        return new StockQuantity(value + amount);
    }

    public StockQuantity subtract(int amount) {
        if (value < amount) {
            throw new IllegalArgumentException("Insufficient stock: has=" + value + ", required=" + amount);
        }
        return new StockQuantity(value - amount);
    }

    public boolean hasEnough(int required) {
        return value >= required;
    }

    public boolean isOutOfStock() {
        return value == 0;
    }
}
