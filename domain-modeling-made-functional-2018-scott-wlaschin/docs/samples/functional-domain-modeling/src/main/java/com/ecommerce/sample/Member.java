package com.ecommerce.sample;

/**
 * Chapter 1.3: 불변성과 Wither 패턴 예제
 * 
 * JEP 468 (Derived Record Creation)이 Java 25에 포함되지 않았으므로,
 * 불변 객체의 부분 수정을 위해 수동으로 'with' 메서드를 구현하는 패턴입니다.
 */
public record Member(String name, int age, String email) {

    // --- Wither Methods (수동 구현) ---

    public Member withName(String newName) {
        return new Member(newName, this.age, this.email);
    }

    public Member withAge(int newAge) {
        return new Member(this.name, newAge, this.email);
    }

    public Member withEmail(String newEmail) {
        return new Member(this.name, this.age, newEmail);
    }
}
