package com.ecommerce.sample;

/**
 * Wither 패턴 예제 - 불변 객체의 부분 수정 (Chapter 1.3)
 *
 * <h2>목적 (Purpose)</h2>
 * 불변 객체에서 일부 필드만 변경한 새 인스턴스를 생성하는 패턴.
 * 원본 객체는 변경되지 않으므로 부수효과 없이 안전하게 사용 가능.
 *
 * <h2>핵심 개념 (Key Concept)</h2>
 * <pre>{@code
 * // 변경 가능 객체 (Mutable) - 원본이 바뀜
 * member.setEmail("new@email.com");  // 원본 member가 변경됨
 *
 * // 불변 객체 (Immutable) - 원본 유지, 새 객체 반환
 * var updated = member.withEmail("new@email.com");  // member는 그대로
 * }</pre>
 *
 * <h2>왜 "with" 접두사인가?</h2>
 * <ul>
 *   <li>setXxx(): 객체 상태를 변경 (mutation)</li>
 *   <li>withXxx(): 새 객체를 "~와 함께" 반환 (immutable)</li>
 *   <li>Java의 LocalDate, Optional 등도 이 패턴 사용</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // 불변성의 이점: 동시성 안전, 예측 가능한 코드
 * var m1 = new Member("Kim", 30, "kim@test.com");
 * var m2 = m1.withAge(31);  // m1은 여전히 age=30
 *
 * // 체이닝 가능
 * var m3 = m1.withAge(31).withEmail("new@test.com");
 * }</pre>
 *
 * <p>참고: JEP 468 (Derived Record Creation)이 Java 25에 포함되지 않아
 * 수동으로 with 메서드를 구현해야 한다.</p>
 */
public record Member(String name, int age, String email) {

    // --- Wither Methods ---
    // 각 메서드는 변경된 필드만 제외하고 나머지는 this에서 복사

    /** 이름만 변경한 새 Member 반환. 원본은 변경되지 않음 */
    public Member withName(String newName) {
        return new Member(newName, this.age, this.email);
    }

    /** 나이만 변경한 새 Member 반환 */
    public Member withAge(int newAge) {
        return new Member(this.name, newAge, this.email);
    }

    /** 이메일만 변경한 새 Member 반환 */
    public Member withEmail(String newEmail) {
        return new Member(this.name, this.age, newEmail);
    }
}
