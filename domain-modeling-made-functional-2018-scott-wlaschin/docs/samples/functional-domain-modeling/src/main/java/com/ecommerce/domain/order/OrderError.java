package com.ecommerce.domain.order;

import com.ecommerce.domain.product.ProductId;

import java.util.List;

/**
 * 주문 도메인 에러
 * sealed interface로 모든 에러 케이스 처리 강제
 */
public sealed interface OrderError
    permits OrderError.EmptyOrder, OrderError.InvalidCustomerId, OrderError.InvalidShippingAddress,
            OrderError.OutOfStock, OrderError.PaymentFailed, OrderError.PaymentDeadlineExceeded,
            OrderError.CannotCancel, OrderError.NotFound, OrderError.InvalidCoupon {

    String message();

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

    record InvalidShippingAddress(String reason) implements OrderError {
        @Override
        public String message() {
            return "배송지 오류: " + reason;
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

    record PaymentDeadlineExceeded(OrderId orderId) implements OrderError {
        @Override
        public String message() {
            return "결제 기한 초과: " + orderId.value();
        }
    }

    record CannotCancel(OrderId orderId, String reason) implements OrderError {
        @Override
        public String message() {
            return "취소 불가 [" + orderId.value() + "]: " + reason;
        }
    }

    record NotFound(OrderId orderId) implements OrderError {
        @Override
        public String message() {
            return "주문을 찾을 수 없습니다: " + orderId.value();
        }
    }

    record InvalidCoupon(String couponCode, String reason) implements OrderError {
        @Override
        public String message() {
            return "쿠폰 오류 [" + couponCode + "]: " + reason;
        }
    }
}

/**
 * 에러 핸들링 유틸리티
 */
final class OrderErrorHandler {
    private OrderErrorHandler() {}

    public static String combineErrors(List<OrderError> errors) {
        return errors.stream()
            .map(OrderError::message)
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }
}
