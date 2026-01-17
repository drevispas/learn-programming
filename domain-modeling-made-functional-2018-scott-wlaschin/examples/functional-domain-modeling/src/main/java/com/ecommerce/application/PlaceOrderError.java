package com.ecommerce.application;

import com.ecommerce.domain.coupon.CouponError;
import com.ecommerce.domain.member.MemberError;
import com.ecommerce.domain.order.OrderError;
import com.ecommerce.domain.payment.PaymentError;
import com.ecommerce.domain.product.ProductError;

import java.util.List;

/**
 * 주문 처리 에러 - 여러 Bounded Context 에러 통합 (Chapter 10)
 *
 * <h2>목적 (Purpose)</h2>
 * 주문 생성 과정에서 발생할 수 있는 모든 에러를 하나의 타입으로 통합한다.
 * 각 Bounded Context(Member, Product, Coupon, Payment, Order)의 에러를 래핑한다.
 *
 * <h2>핵심 개념: Bounded Context 간 에러 통합</h2>
 * <pre>
 * PlaceOrderError (Application Layer)
 *   ├── MemberIssue   → MemberError (Member Context)
 *   ├── ProductIssue  → ProductError (Product Context)
 *   ├── CouponIssue   → CouponError (Coupon Context)
 *   ├── PaymentIssue  → PaymentError (Payment Context)
 *   └── OrderIssue    → OrderError (Order Context)
 * </pre>
 *
 * <h2>왜 이렇게 구현하는가?</h2>
 * <ul>
 *   <li>각 Context의 에러 타입을 그대로 노출하면 Context 간 의존성 발생</li>
 *   <li>Application Layer에서 통합 에러로 변환하여 상위 레이어에 전달</li>
 *   <li>Controller는 PlaceOrderError만 알면 됨 (각 Context 에러 몰라도 됨)</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // Controller에서 에러 처리 - PlaceOrderError 패턴 매칭만 필요
 * return switch (result.error()) {
 *     case MemberIssue e -> ResponseEntity.status(404).body(e.message());
 *     case ProductIssue e -> ResponseEntity.status(409).body(e.message());
 *     case PaymentIssue e -> ResponseEntity.status(402).body(e.message());
 *     // ... exhaustive handling
 * };
 * }</pre>
 *
 * @see com.ecommerce.shared.Result#mapError 에러 타입 변환에 사용
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
