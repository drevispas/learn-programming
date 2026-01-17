package com.travel.domain.coupon;

import java.time.Instant;

/**
 * 쿠폰 상태 - Sum Type
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 각 상태별 필요한 정보만 보유:
 * - Available: 사용 가능 (추가 정보 없음)
 * - Used: 사용됨 (사용 시간)
 * - Expired: 만료됨 (만료 시간)
 * - Cancelled: 취소됨 (취소 시간, 취소 사유)
 * </pre>
 */
public sealed interface CouponStatus permits
        CouponStatus.Available,
        CouponStatus.Used,
        CouponStatus.Expired,
        CouponStatus.Cancelled {

    /**
     * 사용 가능
     */
    record Available() implements CouponStatus {}

    /**
     * 사용됨
     *
     * @param usedAt 사용 시간
     */
    record Used(Instant usedAt) implements CouponStatus {
        public Used {
            if (usedAt == null) usedAt = Instant.now();
        }
    }

    /**
     * 만료됨
     *
     * @param expiredAt 만료 시간
     */
    record Expired(Instant expiredAt) implements CouponStatus {
        public Expired {
            if (expiredAt == null) expiredAt = Instant.now();
        }
    }

    /**
     * 취소됨
     *
     * @param cancelledAt 취소 시간
     * @param reason      취소 사유
     */
    record Cancelled(Instant cancelledAt, String reason) implements CouponStatus {
        public Cancelled {
            if (cancelledAt == null) cancelledAt = Instant.now();
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("취소 사유는 필수입니다");
            }
        }
    }

    /**
     * 최종 상태 여부
     */
    default boolean isFinal() {
        return this instanceof Used || this instanceof Expired || this instanceof Cancelled;
    }

    /**
     * 표시 이름
     */
    default String displayName() {
        return switch (this) {
            case Available a -> "사용 가능";
            case Used u -> "사용됨";
            case Expired e -> "만료됨";
            case Cancelled c -> "취소됨";
        };
    }
}
