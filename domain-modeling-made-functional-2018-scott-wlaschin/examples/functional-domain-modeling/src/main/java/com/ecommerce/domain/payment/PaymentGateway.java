package com.ecommerce.domain.payment;

import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

/**
 * 결제 게이트웨이 인터페이스 - Anti-Corruption Layer (Chapter 8)
 *
 * <h2>목적 (Purpose)</h2>
 * 외부 결제 시스템과의 통신을 추상화한다.
 * 도메인 레이어는 이 인터페이스만 알고, 실제 결제사 API는 모른다.
 *
 * <h2>핵심 개념: Result 반환</h2>
 * 결제/환불 실패가 예상되는 상황이므로 예외가 아닌 Result 타입 반환.
 * 호출자는 성공/실패를 명시적으로 처리해야 한다.
 *
 * <h2>구현 예시</h2>
 * <pre>{@code
 * // 인프라 레이어에서 실제 결제사 API 호출
 * class TossPaymentGateway implements PaymentGateway {
 *     public Result<String, PaymentError> charge(Money amount, PaymentMethod method) {
 *         try {
 *             TossResponse response = tossClient.charge(...);
 *             return Result.success(response.transactionId());
 *         } catch (TossException e) {
 *             return Result.failure(translateError(e));
 *         }
 *     }
 * }
 * }</pre>
 */
public interface PaymentGateway {

    /**
     * 결제 처리
     * @param amount 결제 금액
     * @param method 결제 수단
     * @return 성공 시 거래 ID, 실패 시 PaymentError
     */
    Result<String, PaymentError> charge(Money amount, PaymentMethod method);

    /**
     * 결제 취소 (환불)
     * @param transactionId 원래 거래 ID
     * @param amount 환불 금액
     * @return 성공 시 환불 거래 ID, 실패 시 PaymentError
     */
    Result<String, PaymentError> refund(String transactionId, Money amount);
}
