package com.ecommerce.domain.coupon;

import com.ecommerce.shared.types.Money;

/**
 * 사용된 쿠폰 결과
 */
public record UsedCoupon(Coupon coupon, Money discountAmount) {}
