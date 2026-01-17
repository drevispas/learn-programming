package com.travel.domain.coupon;

import com.travel.domain.member.MemberId;

import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 Repository 인터페이스
 */
public interface CouponRepository {

    Optional<Coupon> findById(String id);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findByOwnerId(MemberId ownerId);

    List<Coupon> findUsableCouponsByOwnerId(MemberId ownerId);

    Coupon save(Coupon coupon);

    boolean existsById(String id);

    boolean existsByCode(String code);
}
