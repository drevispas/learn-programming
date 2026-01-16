package com.ecommerce.sample;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }
    }

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.KRW);

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money divide(int divisor) {
        return new Money(
            this.amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP),
            this.currency
        );
    }

    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, 2, RoundingMode.HALF_UP), this.currency);
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isNegativeOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }
}

enum Currency { KRW, USD, EUR }

record CustomerId(long value) {
    public CustomerId {
        if (value <= 0) throw new IllegalArgumentException("CustomerId must be positive");
    }
}

record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ProductId empty");
    }
}

record CouponCode(String value) {
    public CouponCode {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("CouponCode empty");
    }
}

record ShippingAddress(String value) {
    public ShippingAddress {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ShippingAddress empty");
    }
}

record Quantity(int value) {
    public Quantity {
        if (value < 1) throw new IllegalArgumentException("Quantity must be >= 1");
    }
}

record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("OrderId empty");
    }
}

record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
    Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}

record ValidatedOrder(
    CustomerId customerId,
    List<OrderLine> lines,
    ShippingAddress shippingAddress,
    CouponCode couponCode
) {
    public ValidatedOrder {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(lines);
        Objects.requireNonNull(shippingAddress);
        Objects.requireNonNull(couponCode);
        if (lines.isEmpty()) throw new IllegalArgumentException("Order lines required");
    }
}

record PricedOrder(CustomerId customerId, Money totalAmount) {}

record OrderPlaced(OrderId orderId, Money totalAmount, LocalDateTime occurredAt) {}
