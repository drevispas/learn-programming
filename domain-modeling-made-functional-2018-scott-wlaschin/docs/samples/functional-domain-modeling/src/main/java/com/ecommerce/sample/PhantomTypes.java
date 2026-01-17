package com.ecommerce.sample;

// =====================================================
// Chapter 4.4: Phantom Type Pattern
// 런타임 오버헤드 없이 제네릭 타입 파라미터로 상태를 구분하는 기법
// =====================================================

// 1. 상태 마커 인터페이스 (메서드 없음, 오직 타입 체크용)
sealed interface EmailState permits Unverified, Verified {}

final class Unverified implements EmailState {}
final class Verified implements EmailState {}

/**
 * 이메일 컨테이너
 * @param <S> 상태를 나타내는 Phantom Type (Unverified 또는 Verified)
 */
record Email<S extends EmailState>(String value) {
    
    // 초기 생성 시에는 항상 Unverified 상태
    public static Email<Unverified> of(String value) {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return new Email<>(value);
    }
}

/**
 * 이메일 검증 서비스
 * - Unverified 타입만 입력으로 받음 (컴파일 타임 강제)
 * - Verified 타입을 반환
 */
class EmailVerificationService {
    
    public Result<Email<Verified>, String> verify(Email<Unverified> email, String code) {
        if ("123456".equals(code)) {
            // 검증 성공 시 Verified 타입으로 승격 (새 객체 생성 없이 타입만 변경 가능하지만 Record라 생성)
            return Result.success(new Email<>(email.value()));
        }
        return Result.failure("Invalid code");
    }
}

/**
 * 회원 가입 서비스
 * - Verified 이메일만 입력으로 받음 (Unverified 입력 시 컴파일 에러)
 */
class RegistrationService {
    public Member register(Email<Verified> email, String name) {
        return new Member(name, 20, email.value());
    }
}
