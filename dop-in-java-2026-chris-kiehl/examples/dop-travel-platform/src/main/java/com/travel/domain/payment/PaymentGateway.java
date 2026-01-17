package com.travel.domain.payment;

import com.travel.shared.Result;
import com.travel.shared.types.Money;

/**
 * 결제 게이트웨이 인터페이스 - 포트 (Hexagonal Architecture)
 *
 * <h2>목적 (Purpose)</h2>
 * 외부 PG사 연동을 추상화하여 도메인이 인프라에 의존하지 않도록 함
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Imperative Shell</h2>
 * <pre>
 * [IS] PaymentGateway 구현체는 Imperative Shell:
 * - 외부 API 호출 (I/O)
 * - 네트워크 통신 (부수효과)
 *
 * [Key Point] 인터페이스를 도메인에, 구현을 인프라에:
 *   domain/payment/PaymentGateway.java (인터페이스)
 *   infrastructure/gateway/TossPaymentGateway.java (구현)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 멱등성</h2>
 * <pre>
 * [Key Point] charge(), refund()는 멱등성 키를 받아 중복 처리 방지:
 * - 같은 키로 여러 번 호출해도 한 번만 처리
 * - PG사 API도 멱등성 키 지원하는 경우 전달
 * </pre>
 */
public interface PaymentGateway {

    /**
     * 결제 요청
     *
     * <p>[Ch 7] idempotencyKey로 중복 결제 방지</p>
     *
     * @param payment 결제 정보
     * @return 결제 결과
     */
    Result<PaymentResult, PaymentError> charge(Payment payment);

    /**
     * 환불 요청
     *
     * @param paymentId      결제 ID
     * @param refundAmount   환불 금액
     * @param idempotencyKey 멱등성 키
     * @return 환불 결과
     */
    Result<RefundResult, PaymentError> refund(
            PaymentId paymentId,
            Money refundAmount,
            IdempotencyKey idempotencyKey
    );

    /**
     * 결제 취소 요청
     *
     * @param paymentId 결제 ID
     * @param reason    취소 사유
     * @return 취소 결과
     */
    Result<CancelResult, PaymentError> cancel(PaymentId paymentId, String reason);

    /**
     * 결제 상태 조회
     *
     * @param paymentId 결제 ID
     * @return 결제 상태
     */
    Result<PaymentStatusResult, PaymentError> getStatus(PaymentId paymentId);

    // ============================================
    // 결과 타입들
    // ============================================

    /**
     * 결제 결과
     */
    record PaymentResult(
            String transactionId,
            String approvalNumber,
            java.time.Instant approvedAt
    ) {}

    /**
     * 환불 결과
     */
    record RefundResult(
            String refundId,
            Money refundedAmount,
            java.time.Instant refundedAt
    ) {}

    /**
     * 취소 결과
     */
    record CancelResult(
            String cancelId,
            java.time.Instant cancelledAt
    ) {}

    /**
     * 결제 상태 조회 결과
     */
    record PaymentStatusResult(
            String status,
            String transactionId,
            Money amount,
            java.time.Instant processedAt
    ) {}
}
