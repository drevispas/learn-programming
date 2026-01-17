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
 * 쿠폰 적용 유스케이스 (Chapter 9)
 *
 * <h2>목적 (Purpose)</h2>
 * 쿠폰 적용 워크플로우와 미리보기 기능을 제공한다.
 *
 * <h2>핵심 개념: execute vs preview</h2>
 * <ul>
 *   <li><b>execute()</b>: 쿠폰 실제 사용 - 상태 변경(부수효과) 발생</li>
 *   <li><b>preview()</b>: 할인 금액만 계산 - 부수효과 없음 (순수 함수처럼 동작)</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * UI에서 "쿠폰 적용 시 예상 할인 금액"을 보여줄 때 preview() 사용.
 * 실제 주문 확정 시에만 execute() 호출하여 쿠폰 상태 변경.
 *
 * <pre>{@code
 * // 장바구니 화면에서 쿠폰 미리보기
 * Money discount = applyCouponUseCase.preview("WELCOME10", orderTotal)
 *     .fold(
 *         d -> d,
 *         error -> Money.ZERO  // 에러 시 할인 0원 표시
 *     );
 *
 * // 주문 확정 시 실제 적용
 * applyCouponUseCase.execute(new ApplyCouponCommand(...));
 * }</pre>
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
     * 쿠폰 미리보기 - 부수효과 없이 할인 금액만 계산
     *
     * execute()와 달리 쿠폰 상태를 변경하지 않는다.
     * UI에서 사용자에게 예상 할인 금액을 보여줄 때 사용.
     *
     * @param couponCode 쿠폰 코드
     * @param orderAmount 주문 금액 (할인 계산 기준)
     * @return 할인 금액 또는 에러 (쿠폰 없음, 최소 주문 금액 미달 등)
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
