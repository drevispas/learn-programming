package com.ecommerce.domain.order;

/**
 * 취소 사유 - 유한 개 선택지를 enum으로 표현 (Chapter 4)
 *
 * <h2>핵심 개념: 비즈니스 규칙의 타입화</h2>
 * 취소 사유를 String으로 받으면 "고객 요청", "고객요청", "CUSTOMER_REQUEST" 등
 * 다양한 형태가 발생할 수 있다. enum으로 정의하여 유효한 값만 허용.
 */
public enum CancelReason {
    CUSTOMER_REQUEST("고객 요청"),
    OUT_OF_STOCK("재고 부족"),
    PAYMENT_FAILED("결제 실패"),
    FRAUD_SUSPECTED("사기 의심"),
    PAYMENT_DEADLINE_EXCEEDED("결제 기한 초과");

    private final String description;

    CancelReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
