package com.travel.domain.booking;

import com.travel.shared.types.Money;

import java.time.Instant;

/**
 * 예약 상태 - Sum Type으로 불가능한 상태 제거
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 라이프사이클을 타입으로 모델링하여 잘못된 상태 조합을 컴파일 타임에 방지
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Cardinality - Sum Type</h2>
 * <pre>
 * [Before] boolean 필드들 사용:
 *   boolean isPending, isConfirmed, isPaid, isCancelled, isCompleted
 *   → 2^5 = 32가지 상태 가능 (대부분 무의미/불가능)
 *   예: isPending=true && isCompleted=true → 말이 안 됨
 *
 * [After] sealed interface 사용:
 *   → 정확히 5가지 상태만 가능
 *   각 상태별 필요한 데이터만 보유
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 불가능한 상태 제거</h2>
 * <pre>
 * [Key Point] 각 상태가 해당 상태에서만 의미 있는 데이터 보유:
 * - Pending: 만료 시간 (결제 대기 중이니까)
 * - Confirmed: 결제 ID, 확정 시간
 * - Cancelled: 취소 사유, 환불 금액 (환불이 있을 때만)
 * - Completed: 체크아웃 시간
 *
 * Pending 상태에서 "확정 시간"에 접근? → 컴파일 에러
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 패턴 매칭</h2>
 * <pre>
 * switch (status) {
 *     case Pending p -> "결제 대기 중, 만료: " + p.expiresAt();
 *     case Confirmed c -> "확정됨, 결제 ID: " + c.paymentId();
 *     case Cancelled c -> "취소됨, 사유: " + c.reason();
 *     case Completed c -> "완료, 체크아웃: " + c.completedAt();
 *     case NoShow n -> "노쇼, 발생일: " + n.occurredAt();
 * }
 *
 * [Trap] default 절 사용 금지! 새 상태 추가 시 컴파일러가 누락 알려줌
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] switch에 default 사용 → exhaustiveness 검사 무효화</li>
 *   <li>[Trap] 상태 전이 시 새 객체 반환 안 함 → 불변성 위반</li>
 *   <li>[Why sealed] sealed interface만 Sum Type 구현 가능</li>
 * </ul>
 */
public sealed interface BookingStatus permits
        BookingStatus.Pending,
        BookingStatus.Confirmed,
        BookingStatus.Cancelled,
        BookingStatus.Completed,
        BookingStatus.NoShow {

    // ============================================
    // [Key Point] 각 상태 - 해당 상태에서만 의미 있는 데이터 보유
    // ============================================

    /**
     * 결제 대기 상태
     *
     * <p>예약 생성 직후 상태. 결제 완료 전까지 유지.</p>
     *
     * @param createdAt 예약 생성 시간
     * @param expiresAt 결제 만료 시간 (이 시간까지 결제 안 하면 자동 취소)
     */
    record Pending(
            Instant createdAt,
            Instant expiresAt
    ) implements BookingStatus {
        public Pending {
            if (createdAt == null) throw new IllegalArgumentException("생성 시간은 필수입니다");
            if (expiresAt == null) throw new IllegalArgumentException("만료 시간은 필수입니다");
            if (!expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException("만료 시간은 생성 시간 이후여야 합니다");
            }
        }

        /**
         * 기본 만료 시간(30분)으로 Pending 생성
         */
        public static Pending withDefaultExpiry() {
            Instant now = Instant.now();
            return new Pending(now, now.plusSeconds(30 * 60)); // 30분
        }

        /**
         * 만료 여부 확인
         */
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * 예약 확정 상태
     *
     * <p>결제 완료 후 상태. 실제 서비스 이용 전까지 유지.</p>
     *
     * @param paymentId   결제 ID (확정이 됐으니 반드시 존재)
     * @param confirmedAt 확정 시간
     */
    record Confirmed(
            String paymentId,
            Instant confirmedAt
    ) implements BookingStatus {
        public Confirmed {
            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalArgumentException("결제 ID는 필수입니다");
            }
            if (confirmedAt == null) {
                throw new IllegalArgumentException("확정 시간은 필수입니다");
            }
        }

        /**
         * 현재 시간으로 Confirmed 생성
         */
        public static Confirmed now(String paymentId) {
            return new Confirmed(paymentId, Instant.now());
        }
    }

    /**
     * 예약 취소 상태
     *
     * <p>사용자 요청 또는 시스템에 의한 취소.</p>
     *
     * @param reason       취소 사유
     * @param cancelledAt  취소 시간
     * @param refundAmount 환불 금액 (null이면 환불 없음 - 무료 예약이었거나 환불 불가)
     * @param cancelledBy  취소 주체 (USER, SYSTEM, ADMIN)
     */
    record Cancelled(
            String reason,
            Instant cancelledAt,
            Money refundAmount,
            CancelledBy cancelledBy
    ) implements BookingStatus {
        public Cancelled {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("취소 사유는 필수입니다");
            }
            if (cancelledAt == null) {
                throw new IllegalArgumentException("취소 시간은 필수입니다");
            }
            if (cancelledBy == null) {
                throw new IllegalArgumentException("취소 주체는 필수입니다");
            }
            // refundAmount는 null 가능 (환불 없는 경우)
        }

        /**
         * 취소 주체
         */
        public enum CancelledBy {
            /** 사용자 직접 취소 */
            USER,
            /** 시스템 자동 취소 (결제 만료 등) */
            SYSTEM,
            /** 관리자 취소 */
            ADMIN
        }

        /**
         * 환불이 있는지 확인
         */
        public boolean hasRefund() {
            return refundAmount != null && !refundAmount.isZero();
        }
    }

    /**
     * 이용 완료 상태
     *
     * <p>서비스 이용이 정상 완료된 상태.</p>
     *
     * @param completedAt 완료 시간 (체크아웃 시간 등)
     */
    record Completed(
            Instant completedAt
    ) implements BookingStatus {
        public Completed {
            if (completedAt == null) {
                throw new IllegalArgumentException("완료 시간은 필수입니다");
            }
        }

        /**
         * 현재 시간으로 Completed 생성
         */
        public static Completed now() {
            return new Completed(Instant.now());
        }
    }

    /**
     * 노쇼 상태
     *
     * <p>예약했지만 실제 이용하지 않은 상태.</p>
     *
     * @param occurredAt 노쇼 발생 시간
     * @param penaltyAmount 노쇼 패널티 금액 (null이면 패널티 없음)
     */
    record NoShow(
            Instant occurredAt,
            Money penaltyAmount
    ) implements BookingStatus {
        public NoShow {
            if (occurredAt == null) {
                throw new IllegalArgumentException("노쇼 발생 시간은 필수입니다");
            }
            // penaltyAmount는 null 가능 (패널티 없는 경우)
        }

        /**
         * 패널티가 있는지 확인
         */
        public boolean hasPenalty() {
            return penaltyAmount != null && !penaltyAmount.isZero();
        }
    }

    // ============================================
    // [Key Point] 상태 전이 메서드
    // 허용된 전이만 메서드로 제공
    // ============================================

    /**
     * 예약 확정 (Pending → Confirmed)
     *
     * <p>[Key Point] 상태 전이는 새 객체 반환 (불변성)</p>
     *
     * @param paymentId 결제 ID
     * @return Confirmed 상태
     * @throws IllegalStateException Pending 상태가 아닌 경우
     */
    default Confirmed confirm(String paymentId) {
        return switch (this) {
            case Pending p -> Confirmed.now(paymentId);
            case Confirmed c -> throw new IllegalStateException("이미 확정된 예약입니다");
            case Cancelled c -> throw new IllegalStateException("취소된 예약은 확정할 수 없습니다");
            case Completed c -> throw new IllegalStateException("완료된 예약은 확정할 수 없습니다");
            case NoShow n -> throw new IllegalStateException("노쇼 예약은 확정할 수 없습니다");
        };
    }

    /**
     * 예약 취소
     *
     * @param reason       취소 사유
     * @param refundAmount 환불 금액
     * @param cancelledBy  취소 주체
     * @return Cancelled 상태
     * @throws IllegalStateException 취소 불가능한 상태인 경우
     */
    default Cancelled cancel(String reason, Money refundAmount, Cancelled.CancelledBy cancelledBy) {
        return switch (this) {
            case Pending p -> new Cancelled(reason, Instant.now(), refundAmount, cancelledBy);
            case Confirmed c -> new Cancelled(reason, Instant.now(), refundAmount, cancelledBy);
            case Cancelled c -> throw new IllegalStateException("이미 취소된 예약입니다");
            case Completed c -> throw new IllegalStateException("완료된 예약은 취소할 수 없습니다");
            case NoShow n -> throw new IllegalStateException("노쇼 예약은 취소할 수 없습니다");
        };
    }

    /**
     * 이용 완료
     *
     * @return Completed 상태
     * @throws IllegalStateException 완료 불가능한 상태인 경우
     */
    default Completed complete() {
        return switch (this) {
            case Pending p -> throw new IllegalStateException("미결제 예약은 완료할 수 없습니다");
            case Confirmed c -> Completed.now();
            case Cancelled c -> throw new IllegalStateException("취소된 예약은 완료할 수 없습니다");
            case Completed c -> throw new IllegalStateException("이미 완료된 예약입니다");
            case NoShow n -> throw new IllegalStateException("노쇼 예약은 완료할 수 없습니다");
        };
    }

    /**
     * 노쇼 처리
     *
     * @param penaltyAmount 패널티 금액
     * @return NoShow 상태
     * @throws IllegalStateException 노쇼 처리 불가능한 상태인 경우
     */
    default NoShow markNoShow(Money penaltyAmount) {
        return switch (this) {
            case Pending p -> throw new IllegalStateException("미결제 예약은 노쇼 처리할 수 없습니다");
            case Confirmed c -> new NoShow(Instant.now(), penaltyAmount);
            case Cancelled c -> throw new IllegalStateException("취소된 예약은 노쇼 처리할 수 없습니다");
            case Completed c -> throw new IllegalStateException("완료된 예약은 노쇼 처리할 수 없습니다");
            case NoShow n -> throw new IllegalStateException("이미 노쇼 처리된 예약입니다");
        };
    }

    // ============================================
    // 상태 확인 헬퍼 메서드
    // ============================================

    /**
     * 취소 가능 여부
     */
    default boolean isCancellable() {
        return this instanceof Pending || this instanceof Confirmed;
    }

    /**
     * 환불 가능 여부
     */
    default boolean isRefundable() {
        return this instanceof Confirmed;
    }

    /**
     * 최종 상태 여부 (더 이상 전이 불가)
     */
    default boolean isFinal() {
        return this instanceof Cancelled ||
               this instanceof Completed ||
               this instanceof NoShow;
    }
}
