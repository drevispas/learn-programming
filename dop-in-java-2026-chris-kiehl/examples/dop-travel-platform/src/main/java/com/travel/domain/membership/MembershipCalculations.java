package com.travel.domain.membership;

import com.travel.domain.member.Member;
import com.travel.shared.types.Money;

/**
 * 멤버십 계산 - 순수 함수 (Functional Core)
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core</h2>
 * <pre>
 * [FC] 모든 메서드가 순수 함수:
 * - 외부 의존성 없음
 * - 동일 입력 → 동일 출력
 * </pre>
 */
public final class MembershipCalculations {

    private MembershipCalculations() {}

    /**
     * 현재 자격에 맞는 등급 계산
     *
     * @param totalAmount 총 예약 금액
     * @param bookingCount 예약 횟수
     * @return 자격 등급
     */
    public static MembershipTier calculateEligibleTier(Money totalAmount, int bookingCount) {
        long amount = totalAmount.amount().longValue();

        // 높은 등급부터 확인
        if (amount >= MembershipTier.DIAMOND.requiredAmount() &&
            bookingCount >= MembershipTier.DIAMOND.requiredBookings()) {
            return MembershipTier.DIAMOND;
        }
        if (amount >= MembershipTier.PLATINUM.requiredAmount() &&
            bookingCount >= MembershipTier.PLATINUM.requiredBookings()) {
            return MembershipTier.PLATINUM;
        }
        if (amount >= MembershipTier.GOLD.requiredAmount() &&
            bookingCount >= MembershipTier.GOLD.requiredBookings()) {
            return MembershipTier.GOLD;
        }
        if (amount >= MembershipTier.SILVER.requiredAmount() &&
            bookingCount >= MembershipTier.SILVER.requiredBookings()) {
            return MembershipTier.SILVER;
        }
        return MembershipTier.BRONZE;
    }

    /**
     * 다음 등급까지 필요한 금액
     */
    public static Money amountToNextTier(Member member) {
        MembershipTier currentTier = member.membershipTier();
        if (currentTier.isMaxTier()) {
            return Money.ZERO_KRW;
        }

        MembershipTier nextTier = currentTier.nextTier();
        long currentAmount = member.totalBookingAmount().amount().longValue();
        long needed = nextTier.requiredAmount() - currentAmount;

        return Money.krw(Math.max(0, needed));
    }

    /**
     * 다음 등급까지 필요한 예약 횟수
     */
    public static int bookingsToNextTier(Member member) {
        MembershipTier currentTier = member.membershipTier();
        if (currentTier.isMaxTier()) {
            return 0;
        }

        MembershipTier nextTier = currentTier.nextTier();
        int needed = nextTier.requiredBookings() - member.bookingCount();

        return Math.max(0, needed);
    }

    /**
     * 등급 할인 금액 계산
     */
    public static Money calculateTierDiscount(MembershipTier tier, Money amount) {
        return tier.calculateDiscount(amount);
    }
}
