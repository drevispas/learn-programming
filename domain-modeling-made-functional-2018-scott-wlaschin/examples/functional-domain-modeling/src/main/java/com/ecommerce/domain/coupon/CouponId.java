package com.ecommerce.domain.coupon;

/**
 * 쿠폰 식별자 Value Object (Chapter 2)
 *
 * 불변식: null이거나 공백일 수 없다.
 */
public record CouponId(String value) {
    public CouponId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CouponId must not be blank");
        }
    }
}
