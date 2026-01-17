package com.ecommerce.domain.member;

import com.ecommerce.shared.Result;

import java.time.LocalDateTime;

/**
 * 이메일 인증 상태 - Sum Type으로 State Machine 구현 (Chapter 4, 5)
 *
 * <h2>핵심 개념: 상태를 타입으로 표현</h2>
 * <pre>
 * UnverifiedEmail → verify() → VerifiedEmail
 *                   (성공 시)
 * </pre>
 *
 * <h2>왜 이렇게 구현하는가?</h2>
 * <ul>
 *   <li>검증되지 않은 이메일에만 verify() 호출 가능</li>
 *   <li>검증된 이메일에서 verify() 호출 자체가 불가능 (컴파일 에러)</li>
 *   <li>각 상태에 필요한 데이터만 보유 (verificationCode는 UnverifiedEmail에만)</li>
 * </ul>
 *
 * @see com.ecommerce.sample.PhantomTypes Phantom Type 방식과 비교
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
            // Step 1: 만료 시간 검사
            if (LocalDateTime.now().isAfter(expiresAt)) {
                return Result.failure(new EmailError.CodeExpired());
            }
            // Step 2: 인증 코드 일치 여부 확인
            if (!verificationCode.equals(inputCode)) {
                return Result.failure(new EmailError.InvalidCode());
            }
            // Step 3: 상태 전환 (Unverified → Verified)
            // [State Transition] 반환 타입이 VerifiedEmail - 상태 전환이 타입으로 표현됨
            // [Key Point] UnverifiedEmail에서만 verify() 호출 가능 - VerifiedEmail에는 이 메서드 없음
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
