package com.ecommerce.application;

import com.ecommerce.domain.coupon.CouponError;
import com.ecommerce.domain.member.MemberError;
import com.ecommerce.domain.order.OrderError;
import com.ecommerce.domain.payment.PaymentError;
import com.ecommerce.domain.product.ProductError;

import java.util.List;

/**
 * 주문 처리 중 발생할 수 있는 에러
 * 여러 도메인의 에러를 통합
 */
public sealed interface PlaceOrderError
    permits PlaceOrderError.MemberIssue, PlaceOrderError.ProductIssue,
            PlaceOrderError.CouponIssue, PlaceOrderError.PaymentIssue,
            PlaceOrderError.OrderIssue, PlaceOrderError.ValidationIssue {

    String message();

    record MemberIssue(MemberError error) implements PlaceOrderError {
        @Override
        public String message() {
            return "회원 오류: " + error.message();
        }
    }

    record ProductIssue(ProductError error) implements PlaceOrderError {
        @Override
        public String message() {
            return "상품 오류: " + error.message();
        }
    }

    record CouponIssue(CouponError error) implements PlaceOrderError {
        @Override
        public String message() {
            return "쿠폰 오류: " + error.message();
        }
    }

    record PaymentIssue(PaymentError error) implements PlaceOrderError {
        @Override
        public String message() {
            return "결제 오류: " + error.message();
        }
    }

    record OrderIssue(OrderError error) implements PlaceOrderError {
        @Override
        public String message() {
            return "주문 오류: " + error.message();
        }
    }

    record ValidationIssue(List<String> errors) implements PlaceOrderError {
        @Override
        public String message() {
            return "검증 오류: " + String.join(", ", errors);
        }
    }
}
