package com.ecommerce.domain.coupon;

import com.ecommerce.shared.types.Money;

/**
 * 쿠폰 타입 - 정액 할인, 정률 할인, 무료 배송
 * sealed interface로 타입을 제한하여 exhaustive pattern matching 강제
 */
public sealed interface CouponType
    permits CouponType.FixedAmount, CouponType.Percentage, CouponType.FreeShipping {

    /**
     * 주어진 주문 금액에 대한 할인 금액 계산
     */
    Money calculateDiscount(Money orderAmount);

    /**
     * 정액 할인 쿠폰
     * 할인 금액이 주문 금액보다 큰 경우 주문 금액만큼만 할인
     */
    record FixedAmount(Money discountAmount) implements CouponType {
        public FixedAmount {
            if (discountAmount == null || discountAmount.isNegativeOrZero()) {
                throw new IllegalArgumentException("Discount amount must be positive");
            }
        }

        @Override
        public Money calculateDiscount(Money orderAmount) {
            return orderAmount.isLessThan(discountAmount) ? orderAmount : discountAmount;
        }
    }

    /**
     * 정률 할인 쿠폰
     * 최대 할인 한도 적용
     */
    record Percentage(int rate, Money maxDiscount) implements CouponType {
        public Percentage {
            if (rate < 1 || rate > 100) {
                throw new IllegalArgumentException("Rate must be between 1 and 100");
            }
            if (maxDiscount == null) {
                throw new IllegalArgumentException("Max discount must not be null");
            }
        }

        @Override
        public Money calculateDiscount(Money orderAmount) {
            Money calculated = orderAmount.multiply(rate).divide(100);
            return calculated.isGreaterThan(maxDiscount) ? maxDiscount : calculated;
        }
    }

    /**
     * 무료 배송 쿠폰
     * 배송비 금액만큼 할인
     */
    record FreeShipping(Money shippingFee) implements CouponType {
        public FreeShipping {
            if (shippingFee == null || shippingFee.isNegativeOrZero()) {
                throw new IllegalArgumentException("Shipping fee must be positive");
            }
        }

        @Override
        public Money calculateDiscount(Money orderAmount) {
            return shippingFee;
        }
    }
}
