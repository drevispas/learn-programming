package com.ecommerce.shared.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 금액을 표현하는 Value Object (Chapter 2, 3)
 *
 * <h2>목적 (Purpose)</h2>
 * 원시 타입({@code long}, {@code BigDecimal}) 대신 도메인 개념을 명시적으로 표현한다.
 * "Primitive Obsession" 안티패턴을 피하고, 금액 관련 로직을 응집시킨다.
 *
 * <h2>핵심 개념 (Key Concept): Value Object 특성</h2>
 * <ul>
 *   <li><b>불변성</b>: record로 선언하여 모든 필드가 final. 연산 결과는 새 인스턴스 반환</li>
 *   <li><b>값 동등성</b>: record가 equals/hashCode 자동 생성. 같은 금액이면 같은 객체</li>
 *   <li><b>자기 검증</b>: compact constructor에서 불변식 강제 (음수 금액 불가)</li>
 *   <li><b>통화 안전성</b>: 다른 통화 간 연산 시 예외 발생</li>
 * </ul>
 *
 * <h2>불변식 (Invariants)</h2>
 * <ul>
 *   <li>amount &gt;= 0 (음수 금액 불가)</li>
 *   <li>amount, currency 모두 non-null</li>
 *   <li>같은 통화끼리만 연산 가능</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // Before: 원시 타입 사용 시 문제점
 * long priceKrw = 10000;
 * long priceUsd = 100;
 * long total = priceKrw + priceUsd;  // 컴파일 OK! 하지만 논리적 오류
 *
 * // After: Value Object로 타입 안전성 확보
 * Money priceKrw = Money.krw(10000);
 * Money priceUsd = Money.of(100, Currency.USD);
 * Money total = priceKrw.add(priceUsd);  // IllegalArgumentException: Currency mismatch
 * }</pre>
 *
 * @param amount 금액 (0 이상)
 * @param currency 통화
 */
public record Money(BigDecimal amount, Currency currency) {
    /**
     * Compact Constructor: record의 정규 생성자 실행 전에 검증 로직 수행
     *
     * 이 패턴으로 "유효하지 않은 Money 인스턴스"가 존재할 수 없음을 보장한다.
     * 생성 시점에 불변식을 강제하므로 이후 모든 코드에서 Money는 항상 유효하다.
     */
    public Money {
        // [Invariant] 생성 시점에 불변식 검증 - 유효하지 않은 상태 불가능
        // [Make Illegal States Unrepresentable] null 또는 음수 금액은 존재할 수 없음
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }
    }

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.KRW);

    public static Money of(long amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    /**
     * 불변 연산: 원본을 수정하지 않고 새 인스턴스 반환
     * Value Object의 핵심 특성 - 상태 변경 없이 새 값 생성
     */
    public Money add(Money other) {
        // [Currency Check] 통화 일치 확인 - 다르면 예외
        requireSameCurrency(other);
        // [Immutable] this 변경 없이 새 Money 반환
        // [Value Object] 동등성은 값으로 판단 (record가 equals/hashCode 자동 구현)
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 뺄셈 결과가 음수면 0으로 처리 (음수 금액 불변식 유지)
     * subtract 후 음수가 될 수 있는 도메인 로직이면 Result 반환 고려
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        // [Invariant 유지] 음수 방지를 위해 max(0) 적용 - 불변식 보존
        return new Money(result.max(BigDecimal.ZERO), this.currency);
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

    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isNegativeOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public Money min(Money other) {
        requireSameCurrency(other);
        return this.isLessThan(other) ? this : other;
    }

    public Money max(Money other) {
        requireSameCurrency(other);
        return this.isGreaterThan(other) ? this : other;
    }

    /**
     * 통화 일치 검증: 다른 통화 간 연산은 환율 변환이 필요하므로 예외 처리
     * 환율 변환이 필요하면 별도 서비스에서 처리 후 같은 통화로 변환
     */
    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }
}
