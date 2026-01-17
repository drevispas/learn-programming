package com.ecommerce.domain.member;

import com.ecommerce.shared.Result;

import java.time.LocalDateTime;

/**
 * 이메일 인증 상태 - Phantom Type 대신 Sum Type 사용
 * 검증 안 된 이메일과 검증된 이메일을 타입으로 구분
 */
public sealed interface EmailVerification
    permits EmailVerification.UnverifiedEmail, EmailVerification.VerifiedEmail {

    String email();

    /**
     * 검증 안 된 이메일
     */
    record UnverifiedEmail(
        String email,
        String verificationCode,
        LocalDateTime expiresAt
    ) implements EmailVerification {

        public UnverifiedEmail {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (verificationCode == null || verificationCode.isBlank()) {
                throw new IllegalArgumentException("Verification code required");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("Expires at required");
            }
        }

        /**
         * 인증 코드 검증
         * @param inputCode 입력된 인증 코드
         * @return 성공 시 VerifiedEmail, 실패 시 EmailError
         */
        public Result<VerifiedEmail, EmailError> verify(String inputCode) {
            if (LocalDateTime.now().isAfter(expiresAt)) {
                return Result.failure(new EmailError.CodeExpired());
            }
            if (!verificationCode.equals(inputCode)) {
                return Result.failure(new EmailError.InvalidCode());
            }
            return Result.success(new VerifiedEmail(email, LocalDateTime.now()));
        }

        /**
         * 인증 코드 만료 여부
         */
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * 검증된 이메일
     */
    record VerifiedEmail(
        String email,
        LocalDateTime verifiedAt
    ) implements EmailVerification {

        public VerifiedEmail {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (verifiedAt == null) {
                throw new IllegalArgumentException("VerifiedAt required");
            }
        }
    }

    /**
     * 팩토리 메서드: 새 이메일 등록 (미검증 상태)
     */
    static UnverifiedEmail createUnverified(String email, String verificationCode) {
        return new UnverifiedEmail(
            email,
            verificationCode,
            LocalDateTime.now().plusMinutes(30) // 30분 유효
        );
    }
}

/**
 * 이메일 인증 에러
 */
sealed interface EmailError permits EmailError.CodeExpired, EmailError.InvalidCode {
    String message();

    record CodeExpired() implements EmailError {
        @Override
        public String message() {
            return "인증 코드가 만료되었습니다";
        }
    }

    record InvalidCode() implements EmailError {
        @Override
        public String message() {
            return "인증 코드가 일치하지 않습니다";
        }
    }
}
