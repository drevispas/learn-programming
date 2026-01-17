package com.travel.domain.member;

import java.util.UUID;

/**
 * 회원 식별자 - 강타입 ID
 *
 * <h2>목적 (Purpose)</h2>
 * String이나 Long 대신 전용 타입을 사용하여 ID 혼동 방지
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Identity</h2>
 * <pre>
 * [Key Point] MemberId는 Entity의 식별자:
 * - ID가 같으면 같은 회원
 * - 회원 정보(이름, 등급 등)가 바뀌어도 동일한 회원
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Cardinality</h2>
 * <pre>
 * [Before] findMember(String memberId, String orderId, String paymentId)
 * → 컴파일러가 순서 실수 감지 못함
 *
 * [After] findMember(MemberId memberId, OrderId orderId, PaymentId paymentId)
 * → 타입이 다르므로 순서 바꾸면 컴파일 에러
 * </pre>
 *
 * @param value UUID 값
 */
public record MemberId(UUID value) {

    public MemberId {
        if (value == null) {
            throw new IllegalArgumentException("MemberId는 null일 수 없습니다");
        }
    }

    /**
     * 새로운 MemberId 생성
     */
    public static MemberId generate() {
        return new MemberId(UUID.randomUUID());
    }

    /**
     * 문자열에서 MemberId 생성
     */
    public static MemberId from(String value) {
        try {
            return new MemberId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 MemberId 형식: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
