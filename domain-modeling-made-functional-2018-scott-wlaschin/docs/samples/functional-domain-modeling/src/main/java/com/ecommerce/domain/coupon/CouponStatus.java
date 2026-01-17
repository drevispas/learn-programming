package com.ecommerce.domain.coupon;

import com.ecommerce.domain.order.OrderId;
import java.time.LocalDateTime;

/**
 * 쿠폰 상태 - 발급됨, 사용됨, 만료됨
 * 각 상태마다 필요한 데이터가 다름 (불가능한 상태 제거)
 */
public sealed interface CouponStatus
    permits CouponStatus.Issued, CouponStatus.Used, CouponStatus.Expired {

    /**
     * 발급된 쿠폰 (사용 가능 상태)
     */
    record Issued(LocalDateTime issuedAt, LocalDateTime expiresAt) implements CouponStatus {
        public Issued {
            if (issuedAt == null) {
                throw new IllegalArgumentException("IssuedAt must not be null");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("ExpiresAt must not be null");
            }
            if (expiresAt.isBefore(issuedAt)) {
                throw new IllegalArgumentException("ExpiresAt must be after issuedAt");
            }
        }

        public boolean isValid() {
            return LocalDateTime.now().isBefore(expiresAt);
        }

        public boolean isExpired() {
            return !isValid();
        }
    }

    /**
     * 사용된 쿠폰
     */
    record Used(LocalDateTime usedAt, OrderId orderId) implements CouponStatus {
        public Used {
            if (usedAt == null) {
                throw new IllegalArgumentException("UsedAt must not be null");
            }
            if (orderId == null) {
                throw new IllegalArgumentException("OrderId must not be null");
            }
        }
    }

    /**
     * 만료된 쿠폰
     */
    record Expired(LocalDateTime expiredAt) implements CouponStatus {
        public Expired {
            if (expiredAt == null) {
                throw new IllegalArgumentException("ExpiredAt must not be null");
            }
        }
    }
}
