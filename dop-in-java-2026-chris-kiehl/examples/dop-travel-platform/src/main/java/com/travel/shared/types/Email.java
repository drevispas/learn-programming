package com.travel.shared.types;

import java.util.regex.Pattern;

/**
 * 이메일 - Phantom Types를 활용한 상태 안전 타입
 *
 * <h2>목적 (Purpose)</h2>
 * 이메일 검증 상태를 타입 시스템으로 추적하여 미검증 이메일 사용 방지
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 Phantom Types</h2>
 * <pre>
 * [Key Point] Phantom Type이란?
 * - 타입 파라미터가 런타임에 사용되지 않음 (값으로 존재하지 않음)
 * - 오직 컴파일 타임 검사만을 위해 존재
 * - 제네릭 타입 소거(Type Erasure) 후에도 컴파일러가 타입 검사 수행
 *
 * Email&lt;Unverified&gt; → verify() → Email&lt;Verified&gt;
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 불가능한 상태 제거</h2>
 * <pre>
 * [Before] boolean isVerified 필드 사용:
 *   - 런타임에만 검증 상태 확인 가능
 *   - 미검증 이메일로 발송하는 버그 가능
 *
 * [After] Phantom Types 사용:
 *   - sendWelcomeEmail(Email&lt;Verified&gt; email) 메서드 시그니처로
 *     미검증 이메일 전달 불가능 (컴파일 에러)
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] Raw Type 사용: Email email → 검증 상태 추적 안 됨</li>
 *   <li>[Trap] 직접 생성자 호출: new Email&lt;Verified&gt;(...) → 검증 우회</li>
 *   <li>[Why private] 생성자를 private으로 하고 팩토리 메서드만 제공</li>
 * </ul>
 *
 * @param <S> 검증 상태 (Unverified 또는 Verified) - Phantom Type
 */
public final class Email<S> {

    // ============================================
    // [Key Point] Phantom Type 마커 인터페이스
    // 실제 인스턴스는 생성되지 않음 - 타입 검사용
    // ============================================

    /**
     * 미검증 상태 마커
     *
     * <p>[Why Impl?] sealed interface는 반드시 permitted 구현체가 필요 (Java 문법 제약)</p>
     * <ul>
     *   <li>sealed: 외부에서 {@code class X implements Unverified} 방지 → 타입 안전성 보장</li>
     *   <li>private Impl: 외부 인스턴스화 방지 → Phantom Type은 런타임에 존재하지 않음</li>
     * </ul>
     */
    public sealed interface Unverified permits UnverifiedImpl {}
    private static final class UnverifiedImpl implements Unverified {}

    /**
     * 검증 완료 상태 마커
     */
    public sealed interface Verified permits VerifiedImpl {}
    private static final class VerifiedImpl implements Verified {}

    // ============================================
    // 필드 및 상수
    // ============================================

    /**
     * 이메일 형식 검증 정규식
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private final String value;

    // ============================================
    // [Key Point] Private 생성자
    // 외부에서 임의로 검증 상태 지정 불가
    // ============================================

    /**
     * Private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     *
     * <p>[Why private] 생성자가 public이면 new Email&lt;Verified&gt;("...")로
     * 검증을 우회할 수 있음</p>
     */
    private Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 비어있을 수 없습니다");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + value);
        }
        this.value = value.toLowerCase().trim();
    }

    // ============================================
    // [Key Point] 팩토리 메서드
    // 검증 상태 전이를 메서드로 강제
    // ============================================

    /**
     * 미검증 이메일 생성
     *
     * <p>[Key Point] 처음 생성되는 이메일은 반드시 Unverified 상태</p>
     *
     * @param email 이메일 문자열
     * @return 미검증 이메일
     */
    public static Email<Unverified> unverified(String email) {
        return new Email<>(email);
    }

    /**
     * 이메일 검증 수행
     *
     * <p>[Key Point] Unverified → Verified 전이는 오직 이 메서드를 통해서만 가능</p>
     *
     * <p>[실제 구현에서는] 이메일 발송 → 링크 클릭 확인 등의 프로세스 필요</p>
     *
     * @param email     검증할 이메일
     * @param verifyPin 검증 코드 (실제 시스템에서 발송된 코드)
     * @return 검증된 이메일
     * @throws IllegalArgumentException 검증 코드가 틀린 경우
     */
    public static Email<Verified> verify(Email<Unverified> email, String verifyPin) {
        // [실제 구현] 검증 코드 확인 로직
        if (verifyPin == null || verifyPin.length() != 6) {
            throw new IllegalArgumentException("유효하지 않은 검증 코드입니다");
        }
        // 검증 성공 시 새로운 Verified 타입 인스턴스 반환
        return new Email<>(email.value);
    }

    /**
     * 이미 검증된 이메일 복원 (DB에서 로드 시)
     *
     * <p>[Trap] 이 메서드는 DB에서 검증 완료된 이메일을 로드할 때만 사용.
     * 비즈니스 로직에서 검증 우회 용도로 사용 금지.</p>
     *
     * @param email 이메일 문자열
     * @return 검증된 이메일
     */
    public static Email<Verified> alreadyVerified(String email) {
        return new Email<>(email);
    }

    // ============================================
    // 접근자 메서드
    // ============================================

    /**
     * 이메일 값 반환
     */
    public String value() {
        return value;
    }

    /**
     * 로컬 파트 (@ 앞부분) 반환
     */
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    /**
     * 도메인 파트 (@ 뒷부분) 반환
     */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    // ============================================
    // Object 메서드 재정의
    // ============================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email<?> email = (Email<?>) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
