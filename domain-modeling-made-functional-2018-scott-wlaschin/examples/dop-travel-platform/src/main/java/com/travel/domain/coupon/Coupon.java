package com.travel.domain.coupon;

import com.travel.domain.member.MemberId;
import com.travel.shared.types.Money;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 쿠폰 - 불변 엔티티
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 항등원</h2>
 * <pre>
 * [Key Point] 0원 할인 쿠폰은 항등원:
 *   price - 0원쿠폰 = price
 *   price + 0원쿠폰 = price
 * </pre>
 *
 * @param id             쿠폰 ID
 * @param code           쿠폰 코드
 * @param couponType     쿠폰 유형
 * @param discountAmount 할인 금액 (정액 할인)
 * @param discountPercent 할인율 (정률 할인, 0-100)
 * @param minOrderAmount 최소 주문 금액
 * @param maxDiscountAmount 최대 할인 금액 (정률 할인 시)
 * @param ownerId        소유자 ID
 * @param status         쿠폰 상태
 * @param validFrom      유효 시작일
 * @param validUntil     유효 종료일
 * @param createdAt      생성 시간
 */
public record Coupon(
        String id,
        String code,
        CouponType couponType,
        Money discountAmount,
        int discountPercent,
        Money minOrderAmount,
        Money maxDiscountAmount,
        MemberId ownerId,
        CouponStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        Instant createdAt
) {

    public Coupon {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("쿠폰 ID는 필수입니다");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("쿠폰 코드는 필수입니다");
        if (couponType == null) throw new IllegalArgumentException("쿠폰 유형은 필수입니다");
        if (status == null) throw new IllegalArgumentException("쿠폰 상태는 필수입니다");
        if (validFrom == null) throw new IllegalArgumentException("유효 시작일은 필수입니다");
        if (validUntil == null) throw new IllegalArgumentException("유효 종료일은 필수입니다");
        if (validFrom.isAfter(validUntil)) {
            throw new IllegalArgumentException("유효 시작일이 종료일보다 늦을 수 없습니다");
        }
        if (createdAt == null) createdAt = Instant.now();
    }

    // ============================================
    // 상태 확인 메서드
    // ============================================

    /**
     * 만료 여부
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(validUntil);
    }

    /**
     * 아직 유효하지 않은지 (시작 전)
     */
    public boolean isNotYetValid() {
        return LocalDate.now().isBefore(validFrom);
    }

    /**
     * 현재 유효한지
     */
    public boolean isCurrentlyValid() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(validFrom) && !today.isAfter(validUntil);
    }

    /**
     * 사용 가능한지 (유효하고 미사용 상태)
     */
    public boolean isUsable() {
        return isCurrentlyValid() && status instanceof CouponStatus.Available;
    }

    /**
     * 사용됨 여부
     */
    public boolean isUsed() {
        return status instanceof CouponStatus.Used;
    }

    /**
     * 특정 회원 소유인지
     */
    public boolean isOwnedBy(MemberId memberId) {
        return ownerId != null && ownerId.equals(memberId);
    }

    // ============================================
    // 할인 계산
    // ============================================

    /**
     * 실제 할인 금액 계산
     *
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public Money calculateDiscount(Money orderAmount) {
        // 최소 주문 금액 미달
        if (minOrderAmount != null && orderAmount.amount().compareTo(minOrderAmount.amount()) < 0) {
            return Money.zero(orderAmount.currency());
        }

        return switch (couponType) {
            case FIXED_AMOUNT -> discountAmount != null ? discountAmount : Money.zero(orderAmount.currency());
            case PERCENTAGE -> {
                Money discount = orderAmount.multiplyPercent(discountPercent);
                // 최대 할인 금액 제한
                if (maxDiscountAmount != null && discount.isGreaterThan(maxDiscountAmount)) {
                    yield maxDiscountAmount;
                }
                yield discount;
            }
        };
    }

    // ============================================
    // Wither 패턴
    // ============================================

    /**
     * 사용 처리
     */
    public Coupon markAsUsed() {
        if (!(status instanceof CouponStatus.Available)) {
            throw new IllegalStateException("사용 가능한 상태가 아닙니다");
        }
        return new Coupon(
                id, code, couponType, discountAmount, discountPercent,
                minOrderAmount, maxDiscountAmount, ownerId,
                new CouponStatus.Used(Instant.now()),
                validFrom, validUntil, createdAt
        );
    }

    /**
     * 복구 (취소 시)
     */
    public Coupon restore() {
        if (!(status instanceof CouponStatus.Used)) {
            throw new IllegalStateException("사용된 상태가 아닙니다");
        }
        return new Coupon(
                id, code, couponType, discountAmount, discountPercent,
                minOrderAmount, maxDiscountAmount, ownerId,
                new CouponStatus.Available(),
                validFrom, validUntil, createdAt
        );
    }
}
