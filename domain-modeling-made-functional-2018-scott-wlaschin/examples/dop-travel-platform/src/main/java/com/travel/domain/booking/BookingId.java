package com.travel.domain.booking;

import java.util.UUID;

/**
 * 예약 식별자 - 강타입 ID
 *
 * <h2>목적 (Purpose)</h2>
 * String이나 Long 대신 전용 타입을 사용하여 ID 혼동 방지
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Identity vs Value</h2>
 * <pre>
 * [Key Point] ID는 엔티티의 식별자
 * - 값이 같으면 같은 엔티티를 가리킴
 * - 내용이 아닌 식별자로 동등성 판단
 *
 * [Before] String bookingId, String memberId, String paymentId
 * → 컴파일러가 타입 구분 못 함, 실수로 memberId를 bookingId로 전달 가능
 *
 * [After] BookingId, MemberId, PaymentId
 * → 각각 다른 타입이므로 컴파일 에러로 실수 방지
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Cardinality</h2>
 * <pre>
 * String: 무한대 (아무 문자열이나 가능)
 * BookingId(UUID): 2^128 (실질적으로 충돌 없는 고유 값)
 * </pre>
 *
 * @param value UUID 값
 */
public record BookingId(UUID value) {

    /**
     * Compact Constructor - null 검증
     */
    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException("BookingId는 null일 수 없습니다");
        }
    }

    /**
     * 새로운 BookingId 생성
     */
    public static BookingId generate() {
        return new BookingId(UUID.randomUUID());
    }

    /**
     * 문자열에서 BookingId 생성
     *
     * @param value UUID 문자열
     * @return BookingId
     * @throws IllegalArgumentException 유효하지 않은 UUID 형식
     */
    public static BookingId from(String value) {
        try {
            return new BookingId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 BookingId 형식: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
