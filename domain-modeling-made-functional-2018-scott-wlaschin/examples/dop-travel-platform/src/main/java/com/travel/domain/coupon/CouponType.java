package com.travel.domain.coupon;

/**
 * 쿠폰 유형 - Enum
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 쿠폰 할인 방식:
 * - FIXED_AMOUNT: 정액 할인 (예: 10,000원 할인)
 * - PERCENTAGE: 정률 할인 (예: 10% 할인)
 * </pre>
 */
public enum CouponType {

    /**
     * 정액 할인
     */
    FIXED_AMOUNT("정액 할인"),

    /**
     * 정률 할인
     */
    PERCENTAGE("정률 할인");

    private final String displayName;

    CouponType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
