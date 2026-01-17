package com.ecommerce.sample;

import java.time.LocalDateTime;
import java.util.List;

// =====================================================
// State Machine Pattern - 타입으로 상태 전이를 강제
// =====================================================

// 상태 전이 흐름:
// UnvalidatedOrder → ValidatedOrder → PricedOrder → UnpaidOrder → PaidOrder → ShippingOrder → DeliveredOrder
//                                                  ↓                ↓
//                                            CancelledOrder   CancelledOrder

// === 주문 상태별 Entity ===
// 각 상태가 별도 타입으로 정의되어, 잘못된 상태 전이가 컴파일 타임에 방지됨

/**
 * 미검증 주문 - Command에서 생성된 원시 데이터
 */
record UnvalidatedOrder(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String shippingAddress,
    String couponCode
) {
    // validate()만 호출 가능 - 다른 상태로 직접 전이 불가능
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
 */
record UnpaidOrder(
    OrderId orderId,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    ShippingAddress shippingAddress,
    LocalDateTime paymentDeadline
) {
    // pay()만 호출 가능 - 결제 전이
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
    // startShipping()만 호출 가능
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
    // completeDelivery()만 호출 가능 - 취소 불가
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
    // 더 이상 상태 전이 불가 - 최종 상태
}


/**
 * 취소된 주문 - 최종 상태
 */
record CancelledOrder(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    LocalDateTime cancelledAt,
    CancelReason reason,
    RefundInfo refundInfo  // null이면 환불 없음 (미결제 취소)
) {
    // 더 이상 상태 전이 불가 - 최종 상태
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
