package com.ecommerce.domain.member;

/**
 * 회원 식별자 Value Object (Chapter 2)
 *
 * 불변식: 양수만 허용 (0 이하 불가)
 */
public record MemberId(long value) {
    public MemberId {
        if (value <= 0) {
            throw new IllegalArgumentException("MemberId must be positive");
        }
    }
}
