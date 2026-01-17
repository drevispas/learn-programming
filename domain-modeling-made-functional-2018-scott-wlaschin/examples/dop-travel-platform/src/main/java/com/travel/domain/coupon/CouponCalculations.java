package com.travel.domain.coupon;

import com.travel.shared.types.Currency;
import com.travel.shared.types.Money;

import java.util.List;

/**
 * 쿠폰 계산 - 순수 함수 (Functional Core)
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 항등원 (Identity Element)</h2>
 * <pre>
 * [Key Point] 할인 연산의 항등원:
 *
 * 0원 할인 쿠폰은 덧셈의 항등원:
 *   discount1 + ZERO = discount1
 *   ZERO + discount1 = discount1
 *
 * 쿠폰 목록이 비어있으면 항등원(0원) 반환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 결합법칙</h2>
 * <pre>
 * 여러 쿠폰의 할인 금액 합산:
 *   (discount1 + discount2) + discount3 = discount1 + (discount2 + discount3)
 * </pre>
 */
public final class CouponCalculations {

    private CouponCalculations() {}

    /**
     * 가장 유리한 쿠폰 선택
     *
     * @param coupons     사용 가능한 쿠폰 목록
     * @param orderAmount 주문 금액
     * @return 가장 할인이 큰 쿠폰 (없으면 null)
     */
    public static Coupon selectBestCoupon(List<Coupon> coupons, Money orderAmount) {
        if (coupons == null || coupons.isEmpty()) {
            return null;
        }

        return coupons.stream()
                .filter(Coupon::isUsable)
                .max((c1, c2) -> {
                    Money d1 = c1.calculateDiscount(orderAmount);
                    Money d2 = c2.calculateDiscount(orderAmount);
                    return d1.amount().compareTo(d2.amount());
                })
                .orElse(null);
    }

    /**
     * 여러 쿠폰의 총 할인 금액 계산
     *
     * <p>[Ch 7] 결합법칙 + 항등원 적용</p>
     *
     * @param coupons     쿠폰 목록
     * @param orderAmount 주문 금액
     * @param currency    통화
     * @return 총 할인 금액
     */
    public static Money calculateTotalDiscount(List<Coupon> coupons, Money orderAmount, Currency currency) {
        if (coupons == null || coupons.isEmpty()) {
            // [Ch 7] 항등원 반환
            return Money.zero(currency);
        }

        // [Ch 7] 결합법칙: 순서 무관하게 합산 가능
        return coupons.stream()
                .filter(Coupon::isUsable)
                .map(coupon -> coupon.calculateDiscount(orderAmount))
                .reduce(Money.zero(currency), Money::add);
    }

    /**
     * 쿠폰 적용 후 최종 금액 계산
     *
     * @param orderAmount 주문 금액
     * @param discount    할인 금액
     * @return 최종 금액 (최소 0원)
     */
    public static Money calculateFinalAmount(Money orderAmount, Money discount) {
        if (discount.isGreaterThan(orderAmount)) {
            return Money.zero(orderAmount.currency());
        }
        return orderAmount.subtract(discount);
    }

    /**
     * 최소 주문 금액 충족 여부
     *
     * @param coupon      쿠폰
     * @param orderAmount 주문 금액
     * @return 충족 여부
     */
    public static boolean meetsMinimumOrderAmount(Coupon coupon, Money orderAmount) {
        if (coupon.minOrderAmount() == null) {
            return true;
        }
        return orderAmount.isGreaterThanOrEqual(coupon.minOrderAmount());
    }

    /**
     * 사용 가능한 쿠폰 필터링
     *
     * @param coupons     쿠폰 목록
     * @param orderAmount 주문 금액
     * @return 사용 가능한 쿠폰 목록
     */
    public static List<Coupon> filterUsableCoupons(List<Coupon> coupons, Money orderAmount) {
        if (coupons == null) {
            return List.of();
        }

        return coupons.stream()
                .filter(Coupon::isUsable)
                .filter(coupon -> meetsMinimumOrderAmount(coupon, orderAmount))
                .toList();
    }
}
