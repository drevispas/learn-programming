package com.ecommerce.domain.order;

import com.ecommerce.domain.payment.PaymentMethod;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 Entity - 불변 객체로 구현된 도메인 엔티티 (Chapter 3, 5)
 *
 * <h2>핵심 개념: State Machine as Methods</h2>
 * 상태 전이 메서드(pay, cancel, startShipping, completeDelivery)가
 * Result를 반환하여 실패 가능한 전이를 타입으로 표현한다.
 *
 * <pre>
 * 상태 흐름:
 * Unpaid → Paid → Shipping → Delivered
 *   ↓        ↓
 * Cancelled (환불 없음 / 환불 있음)
 * </pre>
 *
 * <h2>왜 record인가</h2>
 * <ul>
 *   <li>불변성: 상태 변경 시 새 객체 반환 (원본 불변)</li>
 *   <li>값 동등성: 같은 상태면 같은 주문</li>
 *   <li>스레드 안전: 불변이므로 동시성 문제 없음</li>
 * </ul>
 *
 * @see OrderStatus 주문 상태 Sum Type
 */
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderLine> lines,
    ShippingAddress shippingAddress,
    Money subtotal,
    Money discount,
    Money shippingFee,
    Money totalAmount,
    OrderStatus status
) {
    public Order {
        if (id == null) throw new IllegalArgumentException("OrderId required");
        if (customerId == null) throw new IllegalArgumentException("CustomerId required");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("OrderLines required");
        if (shippingAddress == null) throw new IllegalArgumentException("ShippingAddress required");
        if (totalAmount == null) throw new IllegalArgumentException("TotalAmount required");
        if (status == null) throw new IllegalArgumentException("OrderStatus required");
        if (subtotal == null) subtotal = Money.ZERO;
        if (discount == null) discount = Money.ZERO;
        if (shippingFee == null) shippingFee = Money.ZERO;
    }

    // === 상태 전이 메서드 ===

    /**
     * 결제 처리
     */
    public Result<Order, OrderError> pay(PaymentMethod method, String transactionId) {
        // Step 1: 현재 상태가 Unpaid인지 확인 (패턴 매칭으로 상태 검사 + 변수 바인딩)
        if (!(status instanceof OrderStatus.Unpaid unpaid)) {
            return Result.failure(new OrderError.CannotCancel(id, "이미 결제되었거나 취소된 주문입니다"));
        }

        // Step 2: 결제 기한 만료 여부 확인
        if (unpaid.isExpired()) {
            return Result.failure(new OrderError.PaymentDeadlineExceeded(id));
        }

        // Step 3: Paid 상태로 전환 + 결제 시각 기록
        // [Why record] 기존 Order는 불변이므로 새 Order 인스턴스 생성
        Order paidOrder = new Order(
            id, customerId, lines, shippingAddress,
            subtotal, discount, shippingFee, totalAmount,
            new OrderStatus.Paid(LocalDateTime.now(), method, transactionId)
        );
        return Result.success(paidOrder);
    }

    /**
     * 배송 시작
     */
    public Result<Order, OrderError> startShipping(String trackingNumber) {
        if (!(status instanceof OrderStatus.Paid paid)) {
            return Result.failure(new OrderError.CannotCancel(id, "결제 완료된 주문만 배송 시작 가능합니다"));
        }

        Order shippingOrder = new Order(
            id, customerId, lines, shippingAddress,
            subtotal, discount, shippingFee, totalAmount,
            new OrderStatus.Shipping(paid.paidAt(), trackingNumber, LocalDateTime.now())
        );
        return Result.success(shippingOrder);
    }

    /**
     * 배송 완료
     */
    public Result<Order, OrderError> completeDelivery() {
        if (!(status instanceof OrderStatus.Shipping shipping)) {
            return Result.failure(new OrderError.CannotCancel(id, "배송 중인 주문만 배송 완료 처리 가능합니다"));
        }

        Order deliveredOrder = new Order(
            id, customerId, lines, shippingAddress,
            subtotal, discount, shippingFee, totalAmount,
            new OrderStatus.Delivered(shipping.paidAt(), shipping.trackingNumber(), LocalDateTime.now())
        );
        return Result.success(deliveredOrder);
    }

    /**
     * 주문 취소
     */
    public Result<Order, OrderError> cancel(CancelReason reason) {
        // [Pattern] 패턴 매칭으로 상태별 분기 - 컴파일러가 모든 케이스 처리 검증 (exhaustive)
        return switch (status) {
            // Step 1: Unpaid 상태 - 환불 없이 즉시 취소 가능
            case OrderStatus.Unpaid u -> {
                Order cancelled = new Order(
                    id, customerId, lines, shippingAddress,
                    subtotal, discount, shippingFee, totalAmount,
                    new OrderStatus.Cancelled(LocalDateTime.now(), reason)
                );
                yield Result.success(cancelled);
            }
            // Step 2: Paid 상태 - 24시간 이내만 취소 가능, 환불 정보 포함
            case OrderStatus.Paid p -> {
                if (p.paidAt().plusHours(24).isBefore(LocalDateTime.now())) {
                    yield Result.failure(new OrderError.CannotCancel(id, "결제 후 24시간이 지나 취소할 수 없습니다"));
                }
                // [Key Point] Paid 상태에서만 환불 정보 생성 (Unpaid는 환불 불필요)
                RefundInfo refundInfo = new RefundInfo(p.transactionId(), totalAmount, LocalDateTime.now());
                Order cancelled = new Order(
                    id, customerId, lines, shippingAddress,
                    subtotal, discount, shippingFee, totalAmount,
                    new OrderStatus.Cancelled(LocalDateTime.now(), reason, refundInfo)
                );
                yield Result.success(cancelled);
            }
            // Step 3: 취소 불가 상태들 - 명확한 에러 메시지 반환
            case OrderStatus.Shipping s ->
                Result.failure(new OrderError.CannotCancel(id, "배송 중인 주문은 취소할 수 없습니다"));
            case OrderStatus.Delivered d ->
                Result.failure(new OrderError.CannotCancel(id, "배송 완료된 주문은 취소할 수 없습니다"));
            case OrderStatus.Cancelled c ->
                Result.failure(new OrderError.CannotCancel(id, "이미 취소된 주문입니다"));
        };
    }

    // === 상태 확인 메서드 ===

    public boolean canCancel() {
        return status instanceof OrderStatus.Unpaid ||
               (status instanceof OrderStatus.Paid p &&
                p.paidAt().plusHours(24).isAfter(LocalDateTime.now()));
    }

    public boolean isPaid() {
        return status instanceof OrderStatus.Paid;
    }

    public boolean isShipping() {
        return status instanceof OrderStatus.Shipping;
    }

    public boolean isDelivered() {
        return status instanceof OrderStatus.Delivered;
    }

    public boolean isCancelled() {
        return status instanceof OrderStatus.Cancelled;
    }

    // === 팩토리 메서드 ===

    public static Order create(
        OrderId id,
        CustomerId customerId,
        List<OrderLine> lines,
        ShippingAddress shippingAddress,
        Money discount,
        Money shippingFee,
        LocalDateTime paymentDeadline
    ) {
        Money subtotal = lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);

        Money total = subtotal.subtract(discount).add(shippingFee);

        return new Order(
            id,
            customerId,
            lines,
            shippingAddress,
            subtotal,
            discount,
            shippingFee,
            total,
            new OrderStatus.Unpaid(LocalDateTime.now(), paymentDeadline)
        );
    }
}
