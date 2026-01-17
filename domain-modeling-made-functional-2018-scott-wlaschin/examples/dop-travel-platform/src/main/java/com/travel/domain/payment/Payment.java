package com.travel.domain.payment;

import com.travel.domain.booking.BookingId;
import com.travel.domain.member.MemberId;
import com.travel.shared.types.Money;

import java.time.Instant;

/**
 * 결제 - 결제 정보를 담는 불변 엔티티
 *
 * <h2>목적 (Purpose)</h2>
 * 결제 트랜잭션의 전체 정보와 상태를 관리
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 멱등성</h2>
 * <pre>
 * [Key Point] IdempotencyKey를 통한 중복 결제 방지:
 * - 같은 키로 여러 번 요청해도 한 번만 처리
 * - 기존 결제가 있으면 그 결과 반환
 * </pre>
 *
 * @param id             결제 ID
 * @param bookingId      예약 ID
 * @param memberId       회원 ID
 * @param amount         결제 금액
 * @param paymentMethod  결제 수단
 * @param status         결제 상태
 * @param idempotencyKey 멱등성 키
 * @param createdAt      생성 시간
 * @param updatedAt      수정 시간
 */
public record Payment(
        PaymentId id,
        BookingId bookingId,
        MemberId memberId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        IdempotencyKey idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {

    public Payment {
        if (id == null) throw new IllegalArgumentException("결제 ID는 필수입니다");
        if (bookingId == null) throw new IllegalArgumentException("예약 ID는 필수입니다");
        if (memberId == null) throw new IllegalArgumentException("회원 ID는 필수입니다");
        if (amount == null) throw new IllegalArgumentException("결제 금액은 필수입니다");
        if (paymentMethod == null) throw new IllegalArgumentException("결제 수단은 필수입니다");
        if (status == null) throw new IllegalArgumentException("결제 상태는 필수입니다");
        if (idempotencyKey == null) throw new IllegalArgumentException("멱등성 키는 필수입니다");
        if (createdAt == null) throw new IllegalArgumentException("생성 시간은 필수입니다");
        if (updatedAt == null) throw new IllegalArgumentException("수정 시간은 필수입니다");
    }

    // ============================================
    // 결제 상태 - Sum Type
    // ============================================

    /**
     * 결제 상태
     */
    public sealed interface PaymentStatus permits
            PaymentStatus.Pending,
            PaymentStatus.Completed,
            PaymentStatus.Failed,
            PaymentStatus.Cancelled,
            PaymentStatus.Refunded {

        /**
         * 결제 대기 중
         */
        record Pending(Instant requestedAt) implements PaymentStatus {}

        /**
         * 결제 완료
         *
         * @param completedAt     완료 시간
         * @param transactionId   PG사 트랜잭션 ID
         * @param approvalNumber  승인 번호
         */
        record Completed(
                Instant completedAt,
                String transactionId,
                String approvalNumber
        ) implements PaymentStatus {}

        /**
         * 결제 실패
         *
         * @param failedAt 실패 시간
         * @param reason   실패 사유
         */
        record Failed(
                Instant failedAt,
                String reason
        ) implements PaymentStatus {}

        /**
         * 결제 취소
         *
         * @param cancelledAt 취소 시간
         * @param reason      취소 사유
         */
        record Cancelled(
                Instant cancelledAt,
                String reason
        ) implements PaymentStatus {}

        /**
         * 환불 완료
         *
         * @param refundedAt    환불 시간
         * @param refundAmount  환불 금액
         */
        record Refunded(
                Instant refundedAt,
                Money refundAmount
        ) implements PaymentStatus {}
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 새 결제 생성 (Pending 상태)
     */
    public static Payment create(
            BookingId bookingId,
            MemberId memberId,
            Money amount,
            PaymentMethod paymentMethod,
            IdempotencyKey idempotencyKey
    ) {
        Instant now = Instant.now();
        return new Payment(
                PaymentId.generate(),
                bookingId,
                memberId,
                amount,
                paymentMethod,
                new PaymentStatus.Pending(now),
                idempotencyKey,
                now,
                now
        );
    }

    // ============================================
    // Wither 패턴
    // ============================================

    /**
     * 상태 변경
     */
    public Payment withStatus(PaymentStatus newStatus) {
        return new Payment(
                id, bookingId, memberId, amount, paymentMethod,
                newStatus, idempotencyKey, createdAt, Instant.now()
        );
    }

    /**
     * 결제 완료 처리
     */
    public Payment complete(String transactionId, String approvalNumber) {
        return withStatus(new PaymentStatus.Completed(
                Instant.now(), transactionId, approvalNumber
        ));
    }

    /**
     * 결제 실패 처리
     */
    public Payment fail(String reason) {
        return withStatus(new PaymentStatus.Failed(Instant.now(), reason));
    }

    /**
     * 결제 취소 처리
     */
    public Payment cancel(String reason) {
        return withStatus(new PaymentStatus.Cancelled(Instant.now(), reason));
    }

    /**
     * 환불 처리
     */
    public Payment refund(Money refundAmount) {
        return withStatus(new PaymentStatus.Refunded(Instant.now(), refundAmount));
    }

    // ============================================
    // 상태 확인
    // ============================================

    public boolean isPending() {
        return status instanceof PaymentStatus.Pending;
    }

    public boolean isCompleted() {
        return status instanceof PaymentStatus.Completed;
    }

    public boolean isFailed() {
        return status instanceof PaymentStatus.Failed;
    }

    public boolean isCancelled() {
        return status instanceof PaymentStatus.Cancelled;
    }

    public boolean isRefunded() {
        return status instanceof PaymentStatus.Refunded;
    }
}
