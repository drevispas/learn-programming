package com.travel.domain.payment;

import java.time.Instant;
import java.util.UUID;

/**
 * 멱등성 키 - 중복 결제 방지
 *
 * <h2>목적 (Purpose)</h2>
 * 동일한 결제 요청이 여러 번 실행되어도 한 번만 처리되도록 보장
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 멱등성 (Idempotency)</h2>
 * <pre>
 * [Key Point] 멱등성이란?
 * f(f(x)) = f(x)
 * 같은 연산을 여러 번 수행해도 결과가 동일
 *
 * 결제 시나리오:
 * 1. 사용자가 "결제" 버튼 클릭
 * 2. 네트워크 타임아웃으로 응답 못 받음
 * 3. 사용자가 "결제" 버튼 다시 클릭
 * 4. 멱등성 없으면 → 이중 결제!
 *    멱등성 있으면 → 기존 결제 결과 반환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 멱등성 구현 패턴</h2>
 * <pre>
 * 1. 클라이언트가 고유 키 생성 (UUID)
 * 2. 서버에서 키로 중복 체크
 * 3. 이미 처리된 요청이면 기존 결과 반환
 * 4. 처리되지 않은 요청이면 실행 후 결과 저장
 *
 * [Why 클라이언트 생성]
 * - 서버 생성 시 네트워크 문제로 클라이언트가 키를 못 받을 수 있음
 * - 클라이언트 생성 시 재시도해도 동일 키 사용 가능
 * </pre>
 *
 * @param key       고유 키 (클라이언트에서 생성)
 * @param createdAt 생성 시간
 * @param expiresAt 만료 시간 (보통 24시간)
 */
public record IdempotencyKey(
        String key,
        Instant createdAt,
        Instant expiresAt
) {

    /**
     * 기본 만료 시간: 24시간
     */
    public static final long DEFAULT_TTL_HOURS = 24;

    public IdempotencyKey {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 필수입니다");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusSeconds(DEFAULT_TTL_HOURS * 3600);
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 새 멱등성 키 생성 (UUID 기반)
     *
     * <p>[Key Point] 클라이언트에서 호출하여 고유 키 생성</p>
     */
    public static IdempotencyKey generate() {
        return new IdempotencyKey(
                UUID.randomUUID().toString(),
                Instant.now(),
                null
        );
    }

    /**
     * 기존 키 문자열로 IdempotencyKey 생성
     *
     * @param key 키 문자열
     */
    public static IdempotencyKey from(String key) {
        return new IdempotencyKey(key, Instant.now(), null);
    }

    /**
     * 커스텀 TTL로 생성
     *
     * @param ttlHours 만료 시간 (시간 단위)
     */
    public static IdempotencyKey withTtl(long ttlHours) {
        Instant now = Instant.now();
        return new IdempotencyKey(
                UUID.randomUUID().toString(),
                now,
                now.plusSeconds(ttlHours * 3600)
        );
    }

    // ============================================
    // 상태 확인 메서드
    // ============================================

    /**
     * 키가 만료되었는지 확인
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * 키가 유효한지 확인 (만료되지 않음)
     */
    public boolean isValid() {
        return !isExpired();
    }

    @Override
    public String toString() {
        return key;
    }
}
