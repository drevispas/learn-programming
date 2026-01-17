package com.travel.domain.membership;

import com.travel.shared.types.Money;

/**
 * 멤버십 등급 - Sum Type (Enum)
 *
 * <h2>목적 (Purpose)</h2>
 * 회원 등급을 타입으로 표현하고 등급별 혜택 정의
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] Enum은 간단한 Sum Type:
 * - 정해진 값만 가능 (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
 * - 각 값에 연관 데이터 포함 가능
 *
 * [Why Enum] sealed interface 대신 enum 사용:
 * - 단순한 열거형에 적합
 * - 추가 데이터가 인스턴스별로 다르지 않음
 * </pre>
 */
public enum MembershipTier {

    BRONZE("브론즈", 0, 0, 0),
    SILVER("실버", 500_000, 3, 5),
    GOLD("골드", 1_000_000, 10, 10),
    PLATINUM("플래티넘", 3_000_000, 20, 15),
    DIAMOND("다이아몬드", 10_000_000, 50, 20);

    private final String displayName;
    private final long requiredAmount;     // 승급 필요 금액
    private final int requiredBookings;    // 승급 필요 예약 횟수
    private final int discountPercent;     // 할인율

    MembershipTier(String displayName, long requiredAmount, int requiredBookings, int discountPercent) {
        this.displayName = displayName;
        this.requiredAmount = requiredAmount;
        this.requiredBookings = requiredBookings;
        this.discountPercent = discountPercent;
    }

    public String displayName() {
        return displayName;
    }

    public long requiredAmount() {
        return requiredAmount;
    }

    public int requiredBookings() {
        return requiredBookings;
    }

    public int discountPercent() {
        return discountPercent;
    }

    /**
     * 할인 금액 계산
     */
    public Money calculateDiscount(Money amount) {
        return amount.multiplyPercent(discountPercent);
    }

    /**
     * 다음 등급
     */
    public MembershipTier nextTier() {
        return switch (this) {
            case BRONZE -> SILVER;
            case SILVER -> GOLD;
            case GOLD -> PLATINUM;
            case PLATINUM -> DIAMOND;
            case DIAMOND -> DIAMOND; // 최고 등급
        };
    }

    /**
     * 최고 등급인지 확인
     */
    public boolean isMaxTier() {
        return this == DIAMOND;
    }
}
