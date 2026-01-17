package com.ecommerce.domain.coupon;

public record CouponId(String value) {
    public CouponId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CouponId must not be blank");
        }
    }
}
