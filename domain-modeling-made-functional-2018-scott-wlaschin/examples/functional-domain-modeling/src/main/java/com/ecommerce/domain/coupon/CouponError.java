package com.ecommerce.domain.coupon;

import com.ecommerce.shared.types.Money;

/**
 * 쿠폰 도메인 에러 - Sum Type으로 에러 모델링 (Chapter 6)
 *
 * 쿠폰 사용 관련 비즈니스 규칙 위반을 타입으로 표현.
 * 각 에러 케이스가 필요한 데이터(예: MinOrderNotMet의 required, actual)를 포함.
 */
public sealed interface CouponError
    permits CouponError.NotFound, CouponError.AlreadyUsed, CouponError.Expired,
            CouponError.MinOrderNotMet, CouponError.NotAvailable, CouponError.CannotExpire {

    String message();

    record NotFound(String couponCode) implements CouponError {
        @Override
        public String message() {
            return "쿠폰을 찾을 수 없습니다: " + couponCode;
        }
    }

    record AlreadyUsed(CouponId couponId) implements CouponError {
        @Override
        public String message() {
            return "이미 사용된 쿠폰입니다: " + couponId.value();
        }
    }

    record Expired(CouponId couponId) implements CouponError {
        @Override
        public String message() {
            return "만료된 쿠폰입니다: " + couponId.value();
        }
    }

    record MinOrderNotMet(Money required, Money actual) implements CouponError {
        @Override
        public String message() {
            return "최소 주문 금액 미달: 필요=" + required.amount() + ", 실제=" + actual.amount();
        }
    }

    record NotAvailable() implements CouponError {
        @Override
        public String message() {
            return "사용 불가능한 쿠폰입니다";
        }
    }

    record CannotExpire(CouponId couponId) implements CouponError {
        @Override
        public String message() {
            return "만료 처리할 수 없는 쿠폰입니다: " + couponId.value();
        }
    }
}
