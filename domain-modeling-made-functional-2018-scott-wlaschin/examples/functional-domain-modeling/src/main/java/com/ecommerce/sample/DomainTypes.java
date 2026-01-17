package com.ecommerce.sample;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 도메인 타입 모음 - Value Object와 Entity 예제 (Chapter 2, 3)
 *
 * 이 파일은 함수형 도메인 모델링에서 사용하는 주요 타입들의 예제를 담고 있다.
 *
 * <h2>학습 포인트</h2>
 * <ul>
 *   <li><b>Value Object</b>: 불변, 값 동등성, 자기 검증 (Money, CustomerId, ProductId 등)</li>
 *   <li><b>Simple Value</b>: 단일 값을 래핑하여 타입 안전성 확보 (CustomerId, ProductId)</li>
 *   <li><b>Compound Value</b>: 여러 값을 조합한 불변 객체 (OrderLine, ValidatedOrder)</li>
 *   <li><b>Workflow Output</b>: 워크플로우 단계별 상태를 타입으로 표현 (ValidatedOrder → PricedOrder)</li>
 * </ul>
 *
 * <h2>핵심 개념: "Make Illegal States Unrepresentable"</h2>
 * compact constructor에서 불변식을 강제하여, 유효하지 않은 객체가 존재할 수 없게 만든다.
 * 런타임 검증 로직이 타입 시스템으로 이동하여 컴파일 타임 안전성이 높아진다.
 *
 * @see com.ecommerce.shared.types.Money 실제 프로덕션용 Money 클래스
 */

/**
 * 금액 Value Object - 불변성과 통화 안전성 보장
 * 자세한 설명은 {@link com.ecommerce.shared.types.Money} 참조
 */
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

// ============================================================================
// Simple Value Objects: 원시 타입 래핑 (Primitive Obsession 방지)
// ============================================================================

/**
 * 고객 식별자 - long 대신 타입으로 의미 부여
 *
 * <pre>{@code
 * // Before: 어떤 long인지 알 수 없음
 * void findOrder(long customerId, long orderId);  // 순서 바꿔도 컴파일됨!
 *
 * // After: 타입으로 구분
 * void findOrder(CustomerId customerId, OrderId orderId);  // 순서 바꾸면 컴파일 에러
 * }</pre>
 */
record CustomerId(long value) {
    public CustomerId {
        if (value <= 0) throw new IllegalArgumentException("CustomerId must be positive");
    }
}

/** 상품 식별자 - 빈 문자열 방지 */
record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ProductId empty");
    }
}

/** 쿠폰 코드 - 외부에서 받은 코드를 검증하여 래핑 */
record CouponCode(String value) {
    public CouponCode {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("CouponCode empty");
    }
}

/** 배송지 주소 - 실제로는 더 복잡한 구조 (도, 시, 구, 상세주소)가 필요 */
record ShippingAddress(String value) {
    public ShippingAddress {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ShippingAddress empty");
    }
}

/** 수량 - 최소 1개 이상 보장 */
record Quantity(int value) {
    public Quantity {
        if (value < 1) throw new IllegalArgumentException("Quantity must be >= 1");
    }
}

/** 주문 식별자 */
record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("OrderId empty");
    }
}

// ============================================================================
// Compound Value Objects: 여러 값을 조합한 불변 객체
// ============================================================================

/**
 * 주문 항목 - 상품, 수량, 단가의 조합
 * 파생 값(subtotal)은 메서드로 계산하여 일관성 보장
 */
record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
    /** 파생 값: 저장하지 않고 필요할 때 계산 (Single Source of Truth) */
    Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}

// ============================================================================
// Workflow State Types: 워크플로우 단계를 타입으로 표현
// ============================================================================

/**
 * 검증 완료된 주문 - "검증됨" 상태를 타입으로 표현
 *
 * <h2>왜 별도 타입인가?</h2>
 * 검증 전 데이터(UnvalidatedOrder)와 검증 후 데이터(ValidatedOrder)를
 * 다른 타입으로 구분하여, 검증되지 않은 데이터가 다음 단계로 전달되는 것을 방지.
 *
 * <pre>{@code
 * // 타입 시그니처만 봐도 검증이 필요함을 알 수 있다
 * Result<ValidatedOrder, ValidationError> validate(UnvalidatedOrder order);
 * PricedOrder price(ValidatedOrder order);  // 검증된 주문만 받음
 * }</pre>
 */
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

/**
 * 가격 책정 완료된 주문
 * ValidatedOrder에서 가격 계산이 완료된 상태를 나타낸다.
 */
record PricedOrder(CustomerId customerId, Money totalAmount) {}

// ============================================================================
// Domain Events: 워크플로우 완료 시 발생하는 이벤트
// ============================================================================

/**
 * 주문 완료 이벤트 - 워크플로우의 출력
 *
 * 이벤트는 과거형 이름 사용 (OrderPlaced, not PlaceOrder)
 * 발생 시각(occurredAt)을 포함하여 이벤트 소싱에도 활용 가능
 */
record OrderPlaced(OrderId orderId, Money totalAmount, LocalDateTime occurredAt) {}
