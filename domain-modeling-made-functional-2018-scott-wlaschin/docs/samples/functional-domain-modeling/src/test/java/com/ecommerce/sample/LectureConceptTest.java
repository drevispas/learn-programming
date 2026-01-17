package com.ecommerce.sample;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("강의 핵심 개념 테스트 (Ch 1 & Ch 4)")
class LectureConceptTest {

    @Test
    @DisplayName("Ch 1.3: Wither 패턴으로 불변 객체 속성 변경")
    void wither_pattern_creates_new_instance() {
        Member original = new Member("Alice", 30, "alice@example.com");
        
        // age 변경
        Member updated = original.withAge(31);
        
        // 원본 유지 확인
        assertEquals(30, original.age());
        // 변경본 확인
        assertEquals(31, updated.age());
        assertEquals(original.name(), updated.name()); // 다른 필드는 그대로
        
        // 체이닝 확인
        Member chained = original.withName("Bob").withEmail("bob@example.com");
        assertEquals("Bob", chained.name());
        assertEquals("bob@example.com", chained.email());
        assertEquals(30, chained.age());
    }

    @Test
    @DisplayName("Ch 4.4: Phantom Type으로 상태 전이 강제")
    void phantom_type_enforces_workflow() {
        Email<Unverified> unverified = Email.of("test@example.com");
        EmailVerificationService verifier = new EmailVerificationService();
        RegistrationService registrar = new RegistrationService();

        // ❌ 컴파일 에러 시뮬레이션:
        // registrar.register(unverified, "name"); // 컴파일 안 됨!

        // 검증 수행
        Result<Email<Verified>, String> result = verifier.verify(unverified, "123456");
        
        assertTrue(result.isSuccess());
        Email<Verified> verified = result.value();
        
        // ✅ Verified 타입만 register 메서드에 전달 가능
        Member member = registrar.register(verified, "Alice");
        
        assertEquals("test@example.com", member.email());
    }
}
