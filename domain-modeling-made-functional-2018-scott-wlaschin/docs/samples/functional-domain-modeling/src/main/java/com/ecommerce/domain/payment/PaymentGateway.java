package com.ecommerce.domain.payment;

import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

/**
 * 결제 게이트웨이 인터페이스
 * 도메인 레이어에 정의되어 인프라 레이어에서 구현
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
