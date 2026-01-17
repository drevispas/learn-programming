package com.ecommerce.sample;

import java.time.LocalDateTime;
import java.util.List;

/**
 * State Machine Pattern - 타입으로 상태 전이를 강제 (Chapter 5)
 *
 * <h2>목적 (Purpose)</h2>
 * 주문 상태 전이를 타입 시스템으로 강제하여, 잘못된 상태 전이가 컴파일 타임에 방지되도록 한다.
 * "결제 전 주문을 배송 시작하는" 같은 비즈니스 규칙 위반이 코드에서 불가능해진다.
 *
 * <h2>핵심 개념 (Key Concept): 타입 기반 상태 머신</h2>
 * <pre>
 * 상태 전이 흐름:
 * UnvalidatedOrder → ValidatedOrder → PricedOrder → UnpaidOrder → PaidOrder → ShippingOrder → DeliveredOrder
 *                                                   ↓                ↓
 *                                             CancelledOrder   CancelledOrder
 * </pre>
 *
 * 각 상태가 별도 타입이므로, 해당 타입에 정의된 메서드만 호출 가능하다.
 * <ul>
 *   <li>UnpaidOrder → pay(), cancel() 가능</li>
 *   <li>PaidOrder → startShipping(), cancel() 가능 (24시간 이내)</li>
 *   <li>ShippingOrder → completeDelivery() 가능 (취소 불가)</li>
 *   <li>DeliveredOrder, CancelledOrder → 최종 상태, 전이 없음</li>
 * </ul>
 *
 * <h2>왜 이렇게 구현하는가?</h2>
 * <pre>{@code
 * // Before: 런타임 검증 필요
 * void startShipping(Order order) {
 *     if (order.status != OrderStatus.PAID) {
 *         throw new IllegalStateException("미결제 주문은 배송할 수 없습니다");
 *     }
 *     // ...
 * }
 *
 * // After: 타입으로 강제
 * ShippingOrder startShipping(PaidOrder order) {
 *     // PaidOrder만 받으므로 상태 검증 불필요
 *     return new ShippingOrder(...);
 * }
 * }</pre>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <ol>
 *   <li>비즈니스 규칙이 타입에 인코딩됨 → 문서 대신 코드로 규칙 표현</li>
 *   <li>상태 전이 버그가 컴파일 에러로 변환</li>
 *   <li>각 상태에서 가능한 동작이 메서드 시그니처로 명확</li>
 * </ol>
 *
 * @see SumTypes 단일 타입 내 상태 표현 (vs 여기서는 각 상태가 별도 타입)
 */

// =====================================================
// 상태별 Entity 정의
// =====================================================

/**
 * 미검증 주문 - 워크플로우의 시작점
 *
 * 외부에서 들어온 원시 데이터(String)를 담고 있다.
 * validate() 메서드만 호출 가능 - 검증 없이 다음 단계로 진행 불가.
 *
 * 왜 String인가? 외부 입력은 항상 원시 타입으로 받고, 검증 후 도메인 타입으로 변환.
 */
// [Type-safe State Machine] 상태 전환 흐름:
// UnvalidatedOrder → validate() → ValidatedOrder → ... → UnpaidOrder → pay() → PaidOrder
// [Type Enforcement] 각 상태가 별도 타입이라 잘못된 전환 불가능
record UnvalidatedOrder(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String shippingAddress,
    String couponCode
) {
    /**
     * 검증 수행 - 성공 시 ValidatedOrder로 전이
     *
     * Applicative Validation으로 모든 에러를 한 번에 수집.
     * 외부 의존성(validator)은 함수형 인터페이스로 주입받아 테스트 용이.
     *
     * @return Result&lt;ValidatedOrder, List&lt;OrderError&gt;&gt; - 성공 시 검증된 주문, 실패 시 모든 에러 리스트
     */
    public Result<ValidatedOrder, List<OrderError>> validate(
        CustomerValidator customerValidator,
        AddressValidator addressValidator,
        CouponValidator couponValidator
    ) {
        return Validation.combine4(
            validateCustomerId(customerId, customerValidator),
            validateLines(lines),
            validateAddress(shippingAddress, addressValidator),
            validateCouponCode(couponCode, couponValidator),
            ValidatedOrder::new
        ).fold(Result::success, Result::failure);
    }

    private Validation<CustomerId, List<OrderError>> validateCustomerId(
        String id, CustomerValidator validator
    ) {
        try {
            long parsed = Long.parseLong(id);
            if (validator.exists(parsed)) {
                return Validation.valid(new CustomerId(parsed));
            }
            return Validation.invalidOne(new OrderError.InvalidCustomerId(id));
        } catch (NumberFormatException e) {
            return Validation.invalidOne(new OrderError.InvalidCustomerId(id));
        }
    }

    private Validation<List<OrderLine>, List<OrderError>> validateLines(List<UnvalidatedOrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalidOne(new OrderError.EmptyOrder());
        }
        List<OrderLine> validated = lines.stream()
            .map(l -> new OrderLine(l.productId(), l.quantity(), l.unitPrice()))
            .toList();
        return Validation.valid(validated);
    }

    private Validation<ShippingAddress, List<OrderError>> validateAddress(
        String address, AddressValidator validator
    ) {
        if (address == null || address.isBlank()) {
            return Validation.invalidOne(new OrderError.InvalidShippingAddress(address));
        }
        if (validator.isValid(address)) {
            return Validation.valid(new ShippingAddress(address));
        }
        return Validation.invalidOne(new OrderError.InvalidShippingAddress(address));
    }

    private Validation<CouponCode, List<OrderError>> validateCouponCode(
        String code, CouponValidator validator
    ) {
        if (code == null || code.isBlank()) {
            return Validation.invalidOne(
                new OrderError.InvalidCouponCode(code, "쿠폰 코드가 비어있습니다"));
        }
        if (validator.isValid(code)) {
            return Validation.valid(new CouponCode(code));
        }
        return Validation.invalidOne(
            new OrderError.InvalidCouponCode(code, "유효하지 않은 쿠폰"));
    }
}

record UnvalidatedOrderLine(ProductId productId, Quantity quantity, Money unitPrice) {}


/**
 * 미결제 주문 - 결제 대기 중
 *
 * 가능한 전이:
 * - pay() → PaidOrder (성공 시)
 * - cancel() → CancelledOrder (언제든 가능)
 *
 * paymentDeadline 초과 시 결제 불가 - 비즈니스 규칙이 타입 메서드에 인코딩됨
 */
record UnpaidOrder(
    OrderId orderId,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    ShippingAddress shippingAddress,
    LocalDateTime paymentDeadline
) {
    /**
     * 결제 수행 - 결제 기한 검증 후 PaidOrder로 전이
     * 반환 타입이 Result이므로 호출자는 실패 케이스를 반드시 처리해야 함
     */
    // [Input Type] UnpaidOrder만 받음 - 컴파일 타임 상태 제약
    // [Output Type] PaidOrder 반환 - 타입으로 상태 전환 표현
    // [Illegal State] PaidOrder.pay() 같은 잘못된 호출은 컴파일 불가
    public Result<PaidOrder, OrderError> pay(
        PaymentMethod paymentMethod,
        PaymentGateway gateway
    ) {
        if (LocalDateTime.now().isAfter(paymentDeadline)) {
            return Result.failure(new OrderError.PaymentFailed("결제 기한 초과"));
        }

        return gateway.charge(totalAmount, paymentMethod)
            .map(txId -> new PaidOrder(
                orderId,
                customerId,
                lines,
                totalAmount,
                shippingAddress,
                LocalDateTime.now(),
                paymentMethod,
                txId
            ))
            .mapError(e -> new OrderError.PaymentFailed(e));
    }

    // cancel()도 가능
    public CancelledOrder cancel(CancelReason reason) {
        return new CancelledOrder(
            orderId,
            customerId,
            totalAmount,
            LocalDateTime.now(),
            reason,
            null  // 환불 정보 없음 (결제 안 됐으므로)
        );
    }
}


/**
 * 결제 완료 주문 - 배송 대기 중
 *
 * 가능한 전이:
 * - startShipping() → ShippingOrder (항상 성공)
 * - cancel() → CancelledOrder (24시간 이내만 가능, Result 반환)
 *
 * paidAt, paymentMethod, transactionId가 항상 존재 (null 불가) -
 * 결제 완료 상태이므로 이 정보들이 반드시 있어야 함
 */
record PaidOrder(
    OrderId orderId,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    ShippingAddress shippingAddress,
    LocalDateTime paidAt,
    PaymentMethod paymentMethod,
    String transactionId
) {
    // [Input Type] PaidOrder만 받음 - 미결제 주문은 배송 시작 불가 (컴파일 에러)
    // [Output Type] ShippingOrder - 상태 전환이 타입으로 표현됨
    /** 배송 시작 - 항상 성공하므로 Result 없이 바로 ShippingOrder 반환 */
    public ShippingOrder startShipping(String trackingNumber) {
        return new ShippingOrder(
            orderId,
            customerId,
            lines,
            totalAmount,
            shippingAddress,
            paidAt,
            trackingNumber,
            LocalDateTime.now()
        );
    }

    // 24시간 내에만 취소 가능
    public Result<CancelledOrder, OrderError> cancel(CancelReason reason) {
        if (paidAt.plusHours(24).isBefore(LocalDateTime.now())) {
            return Result.failure(
                new OrderError.PaymentFailed("결제 후 24시간이 지나 취소할 수 없습니다"));
        }
        return Result.success(new CancelledOrder(
            orderId,
            customerId,
            totalAmount,
            LocalDateTime.now(),
            reason,
            new RefundInfo(transactionId, totalAmount, LocalDateTime.now())
        ));
    }
}


/**
 * 배송 중 주문
 */
record ShippingOrder(
    OrderId orderId,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    ShippingAddress shippingAddress,
    LocalDateTime paidAt,
    String trackingNumber,
    LocalDateTime shippedAt
) {
    // [State Constraint] completeDelivery()만 호출 가능 - cancel() 없음
    // [Type Enforcement] ShippingOrder에 cancel() 메서드 자체가 없어 취소 시도 불가
    public DeliveredOrder completeDelivery() {
        return new DeliveredOrder(
            orderId,
            customerId,
            lines,
            totalAmount,
            shippingAddress,
            paidAt,
            trackingNumber,
            LocalDateTime.now()
        );
    }
}


/**
 * 배송 완료 주문 - 최종 상태
 */
// [Final State] 상태 전이 메서드 없음 - 더 이상 변경 불가
record DeliveredOrder(
    OrderId orderId,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    ShippingAddress shippingAddress,
    LocalDateTime paidAt,
    String trackingNumber,
    LocalDateTime deliveredAt
) {
    // [Terminal State] 메서드 없음 = 상태 전이 불가능 = 컴파일 타임 보장
}


/**
 * 취소된 주문 - 최종 상태
 */
// [Final State] 상태 전이 메서드 없음 - 더 이상 변경 불가
record CancelledOrder(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    LocalDateTime cancelledAt,
    CancelReason reason,
    RefundInfo refundInfo  // null이면 환불 없음 (미결제 취소)
) {
    // [Terminal State] 메서드 없음 = 상태 전이 불가능 = 컴파일 타임 보장
}

record RefundInfo(String originalTransactionId, Money refundAmount, LocalDateTime refundedAt) {}


// === Validation 인터페이스 ===
// 실제 구현은 인프라 계층에서 제공

@FunctionalInterface
interface CustomerValidator {
    boolean exists(long customerId);
}

@FunctionalInterface
interface AddressValidator {
    boolean isValid(String address);
}

@FunctionalInterface
interface CouponValidator {
    boolean isValid(String couponCode);
}

@FunctionalInterface
interface PaymentGateway {
    Result<String, String> charge(Money amount, PaymentMethod method);
}


// === Validation fold 확장 ===
// Validation에 fold 메서드를 추가하기 위한 확장
final class ValidationExtensions {
    private ValidationExtensions() {}

    public static <S, E, R> R fold(
        Validation<S, E> validation,
        java.util.function.Function<S, R> onValid,
        java.util.function.Function<E, R> onInvalid
    ) {
        return switch (validation) {
            case Valid<S, E> v -> onValid.apply(v.value());
            case Invalid<S, E> i -> onInvalid.apply(i.errors());
        };
    }
}
