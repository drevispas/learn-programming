package com.ecommerce.domain.member;

/**
 * 회원 도메인 에러 - Sum Type으로 에러 모델링 (Chapter 6)
 *
 * 회원 관련 비즈니스 규칙 위반을 타입으로 표현.
 * sealed interface로 완전 열거 보장.
 */
public sealed interface MemberError
    permits MemberError.NotFound, MemberError.EmailNotVerified, MemberError.InsufficientPoints,
            MemberError.InvalidEmail, MemberError.DuplicateEmail {

    String message();

    record NotFound(MemberId memberId) implements MemberError {
        @Override
        public String message() {
            return "회원을 찾을 수 없습니다: " + memberId.value();
        }
    }

    record EmailNotVerified(String email) implements MemberError {
        @Override
        public String message() {
            return "이메일이 인증되지 않았습니다: " + email;
        }
    }

    record InsufficientPoints(int required, int available) implements MemberError {
        @Override
        public String message() {
            return "포인트가 부족합니다: 필요=" + required + ", 보유=" + available;
        }
    }

    record InvalidEmail(String email) implements MemberError {
        @Override
        public String message() {
            return "유효하지 않은 이메일입니다: " + email;
        }
    }

    record DuplicateEmail(String email) implements MemberError {
        @Override
        public String message() {
            return "이미 사용 중인 이메일입니다: " + email;
        }
    }
}
