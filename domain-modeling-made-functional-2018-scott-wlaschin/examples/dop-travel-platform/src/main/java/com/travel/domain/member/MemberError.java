package com.travel.domain.member;

/**
 * 회원 도메인 오류 - Error as Data
 */
public sealed interface MemberError permits
        MemberError.NotFound,
        MemberError.EmailAlreadyExists,
        MemberError.EmailNotVerified,
        MemberError.InvalidEmail {

    String message();
    String code();

    record NotFound(MemberId memberId) implements MemberError {
        @Override
        public String message() {
            return "회원을 찾을 수 없습니다: " + memberId;
        }

        @Override
        public String code() {
            return "MEMBER_NOT_FOUND";
        }
    }

    record EmailAlreadyExists(String email) implements MemberError {
        @Override
        public String message() {
            return "이미 사용 중인 이메일입니다: " + email;
        }

        @Override
        public String code() {
            return "EMAIL_ALREADY_EXISTS";
        }
    }

    record EmailNotVerified(String email) implements MemberError {
        @Override
        public String message() {
            return "이메일 인증이 필요합니다: " + email;
        }

        @Override
        public String code() {
            return "EMAIL_NOT_VERIFIED";
        }
    }

    record InvalidEmail(String email, String reason) implements MemberError {
        @Override
        public String message() {
            return "유효하지 않은 이메일입니다: " + email + " (" + reason + ")";
        }

        @Override
        public String code() {
            return "INVALID_EMAIL";
        }
    }
}
