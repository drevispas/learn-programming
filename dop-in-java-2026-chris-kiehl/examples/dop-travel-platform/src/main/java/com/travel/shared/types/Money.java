package com.travel.shared.types;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액 - 불변 Value Object
 *
 * <h2>목적 (Purpose)</h2>
 * 금액과 통화를 하나의 불변 단위로 관리하여 정확한 금융 계산 보장
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * [Key Point] Value Object의 특징:
 * 1. 불변성 (Immutability) - record로 자동 보장
 * 2. 동등성 기반 비교 - 내용이 같으면 같은 것
 * 3. 자체 검증 - compact constructor에서 유효성 검증
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 항등원 (Identity Element)</h2>
 * <pre>
 * Money.ZERO_KRW는 덧셈의 항등원:
 *   amount + ZERO_KRW = amount
 *   ZERO_KRW + amount = amount
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] double/float 사용 → 부동소수점 오차 발생</li>
 *   <li>[Trap] 다른 통화끼리 직접 연산 → 환율 계산 누락</li>
 *   <li>[Trap] null 금액 허용 → NullPointerException 가능</li>
 * </ul>
 *
 * @param amount   금액 (null 불가, 0 이상)
 * @param currency 통화 (null 불가)
 */
public record Money(
        BigDecimal amount,
        Currency currency
) {
    // ============================================
    // [Key Point] 항등원 상수
    // Ch 7: 덧셈 연산의 항등원으로 활용
    // ============================================

    /**
     * 원화 0원 - 덧셈의 항등원
     */
    public static final Money ZERO_KRW = new Money(BigDecimal.ZERO, Currency.KRW);

    /**
     * 달러 0달러 - 덧셈의 항등원
     */
    public static final Money ZERO_USD = new Money(BigDecimal.ZERO, Currency.USD);

    /**
     * 엔화 0엔 - 덧셈의 항등원
     */
    public static final Money ZERO_JPY = new Money(BigDecimal.ZERO, Currency.JPY);

    // ============================================
    // [Key Point] Compact Constructor
    // Ch 2: Record의 자체 검증 기능
    // ============================================

    /**
     * Compact Constructor - 생성 시점에 유효성 검증
     *
     * <p>[Key Point] Compact Constructor는 Record만의 기능으로,
     * 생성자 파라미터를 그대로 사용하면서 검증 로직 추가 가능</p>
     */
    public Money {
        // [Ch 2] null 검증 - 불변 객체는 생성 시점에 완전해야 함
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (currency == null) {
            throw new IllegalArgumentException("통화는 null일 수 없습니다");
        }
        // [Ch 2] 비즈니스 규칙 - 음수 금액 방지
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
        // [Key Point] 통화별 소수점 정규화
        amount = amount.setScale(currency.decimalPlaces(), RoundingMode.HALF_UP);
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 원화 금액 생성
     *
     * @param amount 금액 (정수)
     * @return Money 객체
     */
    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    /**
     * 달러 금액 생성
     *
     * @param amount 금액 (소수점 2자리까지)
     * @return Money 객체
     */
    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.USD);
    }

    /**
     * 지정된 통화의 0 금액 반환 - 항등원
     *
     * @param currency 통화
     * @return 해당 통화의 0 금액
     */
    public static Money zero(Currency currency) {
        return switch (currency) {
            case KRW -> ZERO_KRW;
            case USD -> ZERO_USD;
            case JPY -> ZERO_JPY;
        };
    }

    // ============================================
    // [Key Point] 연산 메서드 - 불변성 유지
    // Ch 2: Wither 패턴 응용 - 새 객체 반환
    // ============================================

    /**
     * 금액 더하기
     *
     * <p>[Key Point] 불변 객체이므로 연산 결과는 새 객체로 반환</p>
     * <p>[Trap] 다른 통화끼리 더하기 시도 시 예외 발생</p>
     *
     * @param other 더할 금액
     * @return 합계 (새 Money 객체)
     * @throws IllegalArgumentException 통화가 다른 경우
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 금액 빼기
     *
     * <p>[Trap] 결과가 음수가 되면 예외 발생 (비즈니스 규칙)</p>
     *
     * @param other 뺄 금액
     * @return 차액 (새 Money 객체)
     * @throws IllegalArgumentException 통화가 다르거나 결과가 음수인 경우
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "결과가 음수입니다: " + this + " - " + other + " = " + result);
        }
        return new Money(result, this.currency);
    }

    /**
     * 백분율 곱하기 (할인율, 수수료율 등)
     *
     * @param percentage 백분율 (예: 10 = 10%)
     * @return 계산 결과
     */
    public Money multiplyPercent(int percentage) {
        BigDecimal multiplier = BigDecimal.valueOf(percentage)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * 정수 곱하기
     *
     * @param multiplier 승수
     * @return 계산 결과
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    // ============================================
    // 비교 메서드
    // ============================================

    /**
     * 0원인지 확인
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 다른 금액보다 큰지 확인
     */
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    /**
     * 다른 금액 이상인지 확인
     */
    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    // ============================================
    // 유틸리티 메서드
    // ============================================

    /**
     * 통화가 같은지 검증
     */
    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    "통화가 다릅니다: " + this.currency + " vs " + other.currency);
        }
    }

    /**
     * 포맷된 문자열 (예: "₩10,000")
     */
    public String formatted() {
        return currency.symbol() + String.format("%,.0f", amount);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
