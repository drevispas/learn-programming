package com.ecommerce.domain.coupon;

import java.util.Optional;

/**
 * 쿠폰 Repository 인터페이스 - Persistence Ignorance (Chapter 8)
 *
 * 도메인 레이어에서 정의, 인프라 레이어에서 구현.
 */
public interface CouponRepository {
    Optional<Coupon> findById(CouponId id);
    Optional<Coupon> findByCode(String code);
    Coupon save(Coupon coupon);
    void delete(CouponId id);
}
