package com.ecommerce.application;

import com.ecommerce.domain.order.CancelReason;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderError;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.domain.order.OrderRepository;
import com.ecommerce.domain.order.OrderStatus;
import com.ecommerce.domain.payment.PaymentError;
import com.ecommerce.domain.payment.PaymentGateway;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 주문 취소 유스케이스 - Imperative Shell (Chapter 9)
 *
 * <h2>목적 (Purpose)</h2>
 * 주문 취소 워크플로우를 조율한다. 상태에 따라 환불 처리 여부가 결정된다.
 *
 * <h2>핵심 개념: 상태별 다른 처리</h2>
 * <ul>
 *   <li>미결제 취소 → 환불 없음 (결제된 적 없으므로)</li>
 *   <li>결제 완료 취소 → PaymentGateway 환불 처리 필요</li>
 *   <li>배송 중/완료 → 취소 불가 (Order.cancel()에서 에러 반환)</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * 비즈니스 규칙(취소 가능 여부)은 Order 엔티티의 cancel() 메서드에 있고,
 * 이 유스케이스는 외부 시스템(PaymentGateway) 호출만 담당한다.
 *
 * @see com.ecommerce.domain.order.Order#cancel 취소 가능 여부 판단 로직
 */
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public CancelOrderUseCase(OrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * 주문 취소 명령
     */
    public record CancelOrderCommand(
        String orderId,
        CancelReason reason
    ) {
        public CancelOrderCommand {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("OrderId required");
            }
            if (reason == null) {
                throw new IllegalArgumentException("CancelReason required");
            }
        }
    }

    /**
     * 주문 취소 결과
     */
    public record OrderCancelled(
        OrderId orderId,
        CancelReason reason,
        Money refundAmount,
        LocalDateTime cancelledAt
    ) {}

    /**
     * 주문 취소 실행
     */
    public Result<OrderCancelled, PlaceOrderError> execute(CancelOrderCommand command) {
        OrderId orderId = new OrderId(command.orderId());

        // 1. 주문 조회
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return Result.failure(new PlaceOrderError.OrderIssue(
                new OrderError.NotFound(orderId)
            ));
        }
        Order order = orderOpt.get();

        // 2. 취소 가능 여부 확인 및 취소 처리
        Result<Order, OrderError> cancelResult = order.cancel(command.reason());
        if (cancelResult.isFailure()) {
            return Result.failure(new PlaceOrderError.OrderIssue(cancelResult.error()));
        }
        Order cancelledOrder = cancelResult.value();

        // 3. 결제 취소 (환불) 처리 - 결제된 경우만
        Money refundAmount = Money.ZERO;
        if (cancelledOrder.status() instanceof OrderStatus.Cancelled cancelled &&
            cancelled.hasRefund()) {

            Result<String, PaymentError> refundResult = paymentGateway.refund(
                cancelled.refundInfo().originalTransactionId(),
                cancelled.refundInfo().refundAmount()
            );
            if (refundResult.isFailure()) {
                return Result.failure(new PlaceOrderError.PaymentIssue(refundResult.error()));
            }
            refundAmount = cancelled.refundInfo().refundAmount();
        }

        // 4. 주문 저장
        orderRepository.save(cancelledOrder);

        // 5. 결과 반환
        return Result.success(new OrderCancelled(
            orderId,
            command.reason(),
            refundAmount,
            LocalDateTime.now()
        ));
    }
}
