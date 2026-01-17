package com.ecommerce.domain.order;

/**
 * 취소 사유
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
