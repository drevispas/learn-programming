package com.ecommerce.domain.member;

public record MemberId(long value) {
    public MemberId {
        if (value <= 0) {
            throw new IllegalArgumentException("MemberId must be positive");
        }
    }
}
