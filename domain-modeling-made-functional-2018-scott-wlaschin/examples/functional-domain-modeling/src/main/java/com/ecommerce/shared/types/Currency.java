package com.ecommerce.shared.types;

import java.math.BigDecimal;

/**
 * 통화를 표현하는 Value Object (Chapter 2)
 *
 * <h2>핵심 개념</h2>
 * <ul>
 *   <li><b>displayName</b>: 사용자에게 보여줄 통화 이름</li>
 *   <li><b>decimalPlaces</b>: 해당 통화의 소수점 자릿수 (예: KRW=0, USD=2)</li>
 *   <li><b>format()</b>: 금액을 해당 통화 형식에 맞게 포맷팅</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * Currency.KRW.format(BigDecimal.valueOf(10000));  // "10,000원"
 * Currency.USD.format(BigDecimal.valueOf(100.50)); // "$100.50"
 * Currency.JPY.format(BigDecimal.valueOf(1000));   // "¥1,000"
 * }</pre>
 */
public enum Currency {
    KRW("원", 0),      // 소수점 없음
    USD("달러", 2),    // 소수점 2자리
    EUR("유로", 2),    // 소수점 2자리
    JPY("엔", 0);      // 소수점 없음

    private final String displayName;
    private final int decimalPlaces;

    Currency(String displayName, int decimalPlaces) {
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * 해당 통화의 소수점 자릿수
     *
     * @return 소수점 자릿수 (예: KRW=0, USD=2)
     */
    public int decimalPlaces() {
        return decimalPlaces;
    }

    /**
     * 금액을 해당 통화 형식에 맞게 포맷팅
     *
     * @param amount 포맷팅할 금액
     * @return 포맷팅된 문자열
     */
    public String format(BigDecimal amount) {
        return switch (this) {
            case KRW -> String.format("%,.0f%s", amount, displayName);
            case USD -> String.format("$%,." + decimalPlaces + "f", amount);
            case EUR -> String.format("€%,." + decimalPlaces + "f", amount);
            case JPY -> String.format("¥%,.0f", amount);
        };
    }
}
