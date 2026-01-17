package com.ecommerce.application;

import com.ecommerce.domain.coupon.Coupon;
import com.ecommerce.domain.coupon.CouponError;
import com.ecommerce.domain.coupon.CouponRepository;
import com.ecommerce.domain.coupon.UsedCoupon;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

import java.util.Optional;

/**
 * 쿠폰 적용 유스케이스
 */
public class ApplyCouponUseCase {

    private final CouponRepository couponRepository;

    public ApplyCouponUseCase(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * 쿠폰 적용 명령
     */
    public record ApplyCouponCommand(
        String couponCode,
        String orderId,
        Money orderAmount
    ) {
        public ApplyCouponCommand {
            if (couponCode == null || couponCode.isBlank()) {
                throw new IllegalArgumentException("CouponCode required");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("OrderId required");
            }
            if (orderAmount == null) {
                throw new IllegalArgumentException("OrderAmount required");
            }
        }
    }

    /**
     * 쿠폰 적용 결과
     */
    public record CouponApplied(
        String couponCode,
        Money discountAmount,
        Money originalAmount,
        Money finalAmount
    ) {}

    /**
     * 쿠폰 적용 실행
     */
    public Result<CouponApplied, CouponError> execute(ApplyCouponCommand command) {
        // 1. 쿠폰 조회
        Optional<Coupon> couponOpt = couponRepository.findByCode(command.couponCode());
        if (couponOpt.isEmpty()) {
            return Result.failure(new CouponError.NotFound(command.couponCode()));
        }
        Coupon coupon = couponOpt.get();

        // 2. 쿠폰 사용 시도
        OrderId orderId = new OrderId(command.orderId());
        Result<UsedCoupon, CouponError> useResult = coupon.use(orderId, command.orderAmount());
        if (useResult.isFailure()) {
            return Result.failure(useResult.error());
        }
        UsedCoupon usedCoupon = useResult.value();

        // 3. 쿠폰 저장 (상태 변경)
        couponRepository.save(usedCoupon.coupon());

        // 4. 결과 반환
        return Result.success(new CouponApplied(
            command.couponCode(),
            usedCoupon.discountAmount(),
            command.orderAmount(),
            command.orderAmount().subtract(usedCoupon.discountAmount())
        ));
    }

    /**
     * 쿠폰 미리보기 (사용하지 않고 할인 금액만 계산)
     */
    public Result<Money, CouponError> preview(String couponCode, Money orderAmount) {
        // 1. 쿠폰 조회
        Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
        if (couponOpt.isEmpty()) {
            return Result.failure(new CouponError.NotFound(couponCode));
        }
        Coupon coupon = couponOpt.get();

        // 2. 사용 가능 여부 확인
        if (!coupon.isAvailable()) {
            return Result.failure(new CouponError.NotAvailable());
        }

        // 3. 최소 주문 금액 확인
        if (orderAmount.isLessThan(coupon.minOrderAmount())) {
            return Result.failure(new CouponError.MinOrderNotMet(coupon.minOrderAmount(), orderAmount));
        }

        // 4. 할인 금액 계산
        Money discount = coupon.type().calculateDiscount(orderAmount);
        return Result.success(discount);
    }
}
