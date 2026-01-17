package com.travel.shared.types;

/**
 * 통화 타입 - Value Object Enum
 *
 * <h2>목적 (Purpose)</h2>
 * 지원되는 통화 종류를 타입으로 제한하여 잘못된 통화 코드 사용 방지
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * - Enum은 본질적으로 Value Object (동등성 기반 비교)
 * - 가능한 값을 컴파일 타임에 제한
 * - [Why Enum] String 대신 Enum 사용으로 오타 방지 및 IDE 지원
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Cardinality</h2>
 * <pre>
 * String currency: 무한대 가능 (오타, 잘못된 코드 포함)
 * Currency enum: 정확히 N개만 가능 (현재 3개)
 * </pre>
 */
public enum Currency {

    /**
     * 대한민국 원화
     */
    KRW("KRW", "₩", 0),

    /**
     * 미국 달러
     */
    USD("USD", "$", 2),

    /**
     * 일본 엔화
     */
    JPY("JPY", "¥", 0);

    private final String code;
    private final String symbol;
    private final int decimalPlaces;

    Currency(String code, String symbol, int decimalPlaces) {
        this.code = code;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    /**
     * ISO 4217 통화 코드
     */
    public String code() {
        return code;
    }

    /**
     * 통화 기호 (예: ₩, $, ¥)
     */
    public String symbol() {
        return symbol;
    }

    /**
     * 소수점 자릿수 (KRW, JPY는 0, USD는 2)
     */
    public int decimalPlaces() {
        return decimalPlaces;
    }
}
