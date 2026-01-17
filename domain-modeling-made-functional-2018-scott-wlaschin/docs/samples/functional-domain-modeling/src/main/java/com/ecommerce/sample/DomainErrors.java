package com.ecommerce.sample;

import java.util.List;

// =====================================================
// Domain Error Types - 도메인 특화 에러를 sealed interface로 정의
// =====================================================

// === 주문 에러 (Order Error) ===
// String 대신 구체적인 에러 타입을 사용하여 컴파일 타임에 모든 에러 케이스 처리 강제
sealed interface OrderError permits
    OrderError.EmptyOrder,
    OrderError.InvalidCustomerId,
    OrderError.InvalidShippingAddress,
    OrderError.InvalidCouponCode,
    OrderError.OutOfStock,
    OrderError.PaymentFailed {

    record EmptyOrder() implements OrderError {
        @Override
        public String message() {
            return "주문 상품이 없습니다";
        }
    }

    record InvalidCustomerId(String customerId) implements OrderError {
        @Override
        public String message() {
            return "유효하지 않은 고객 ID: " + customerId;
        }
    }

    record InvalidShippingAddress(String address) implements OrderError {
        @Override
        public String message() {
            return "유효하지 않은 배송 주소: " + address;
        }
    }

    record InvalidCouponCode(String code, String reason) implements OrderError {
        @Override
        public String message() {
            return "쿠폰 오류 [" + code + "]: " + reason;
        }
    }

    record OutOfStock(ProductId productId, int requested, int available) implements OrderError {
        @Override
        public String message() {
            return "재고 부족 [" + productId.value() + "]: 요청=" + requested + ", 재고=" + available;
        }
    }

    record PaymentFailed(String reason) implements OrderError {
        @Override
        public String message() {
            return "결제 실패: " + reason;
        }
    }

    // 공통 메서드
    String message();
}


// === 에러 핸들링 유틸리티 ===
final class OrderErrorHandler {

    private OrderErrorHandler() {}

    // 에러 타입에 따른 처리 - 모든 케이스 처리 강제됨
    public static String handleError(OrderError error) {
        return switch (error) {
            case OrderError.EmptyOrder e -> e.message();
            case OrderError.InvalidCustomerId e -> e.message();
            case OrderError.InvalidShippingAddress e -> e.message();
            case OrderError.InvalidCouponCode e -> e.message();
            case OrderError.OutOfStock e -> e.message();
            case OrderError.PaymentFailed e -> e.message();
        };
    }

    // 여러 에러를 하나의 메시지로 결합
    public static String combineErrors(List<OrderError> errors) {
        return errors.stream()
            .map(OrderError::message)
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }
}
