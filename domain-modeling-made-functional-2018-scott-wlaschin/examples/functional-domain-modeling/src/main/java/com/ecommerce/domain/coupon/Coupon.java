package com.ecommerce.domain.coupon;

import com.ecommerce.domain.order.OrderId;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 쿠폰 Entity - State Machine 패턴 (Chapter 5)
 *
 * <h2>핵심 개념: 상태 전이를 타입으로 표현</h2>
 * <pre>
 * Issued → use() → Used
 *    ↓
 * Expired (유효기간 만료)
 * </pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * Result<UsedCoupon, CouponError> result = coupon.use(orderId, orderAmount);
 *
 * return result.fold(
 *     used -> applyDiscount(used.discountAmount()),
 *     error -> showError(error.message())
 * );
 * }</pre>
 *
 * @see CouponStatus 쿠폰 상태 Sum Type
 * @see CouponType 쿠폰 유형 (정액, 정률, 무료배송)
 */
public record Coupon(
    CouponId id,
    String code,
    CouponType type,
    CouponStatus status,
    Money minOrderAmount
) {
    public Coupon {
        if (id == null) throw new IllegalArgumentException("CouponId must not be null");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon code must not be blank");
        if (type == null) throw new IllegalArgumentException("CouponType must not be null");
        if (status == null) throw new IllegalArgumentException("CouponStatus must not be null");
        if (minOrderAmount == null) throw new IllegalArgumentException("MinOrderAmount must not be null");
    }

    /**
     * 쿠폰 사용 시도
     * @param orderId 주문 ID
     * @param orderAmount 주문 금액
     * @return 성공 시 UsedCoupon, 실패 시 CouponError
     */
    public Result<UsedCoupon, CouponError> use(OrderId orderId, Money orderAmount) {
        // 1. 쿠폰 상태 확인
        if (!(status instanceof CouponStatus.Issued issued)) {
            return switch (status) {
                case CouponStatus.Used u -> Result.failure(new CouponError.AlreadyUsed(id));
                case CouponStatus.Expired e -> Result.failure(new CouponError.Expired(id));
                default -> Result.failure(new CouponError.NotAvailable());
            };
        }

        // 2. 유효기간 확인
        if (issued.isExpired()) {
            return Result.failure(new CouponError.Expired(id));
        }

        // 3. 최소 주문 금액 확인
        if (orderAmount.isLessThan(minOrderAmount)) {
            return Result.failure(new CouponError.MinOrderNotMet(minOrderAmount, orderAmount));
        }

        // 4. 할인 금액 계산
        Money discount = type.calculateDiscount(orderAmount);

        // 5. 쿠폰 상태 변경 (사용됨)
        Coupon usedCoupon = new Coupon(
            id,
            code,
            type,
            new CouponStatus.Used(LocalDateTime.now(), orderId),
            minOrderAmount
        );

        return Result.success(new UsedCoupon(usedCoupon, discount));
    }

    /**
     * 쿠폰이 사용 가능한지 확인
     */
    public boolean isAvailable() {
        return status instanceof CouponStatus.Issued issued && issued.isValid();
    }

    /**
     * 쿠폰 만료 처리
     */
    public Coupon expire() {
        if (!(status instanceof CouponStatus.Issued)) {
            throw new IllegalStateException("이미 사용되었거나 만료된 쿠폰입니다");
        }
        return new Coupon(
            id,
            code,
            type,
            new CouponStatus.Expired(LocalDateTime.now()),
            minOrderAmount
        );
    }

    /**
     * 팩토리 메서드: 새 쿠폰 발급
     */
    public static Coupon issue(
        CouponId id,
        String code,
        CouponType type,
        Money minOrderAmount,
        LocalDateTime expiresAt
    ) {
        return new Coupon(
            id,
            code,
            type,
            new CouponStatus.Issued(LocalDateTime.now(), expiresAt),
            minOrderAmount
        );
    }
}
