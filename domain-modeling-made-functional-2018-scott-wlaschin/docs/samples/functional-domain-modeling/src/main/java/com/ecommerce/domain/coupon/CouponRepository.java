package com.ecommerce.domain.coupon;

import java.util.Optional;

/**
 * 쿠폰 레포지토리 인터페이스
 * 도메인 레이어에 정의되어 인프라 레이어에서 구현
 */
public interface CouponRepository {
    Optional<Coupon> findById(CouponId id);
    Optional<Coupon> findByCode(String code);
    Coupon save(Coupon coupon);
    void delete(CouponId id);
}
