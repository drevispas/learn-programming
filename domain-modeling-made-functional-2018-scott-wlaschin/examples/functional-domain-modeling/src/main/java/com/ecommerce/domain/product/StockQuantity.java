package com.ecommerce.domain.product;

/**
 * 재고 수량 Value Object
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
