package com.ecommerce.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.domain.order.*;
import com.ecommerce.domain.payment.*;
import com.ecommerce.domain.product.ProductId;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

@DisplayName("CancelOrderUseCase 테스트")
class CancelOrderUseCaseTest {

    private CancelOrderUseCase useCase;
    private InMemoryOrderRepository orderRepository;
    private MockPaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        paymentGateway = new MockPaymentGateway();
        useCase = new CancelOrderUseCase(orderRepository, paymentGateway);
    }

    @Nested
    @DisplayName("미결제 주문 취소")
    class UnpaidOrderCancellation {

        @Test
        @DisplayName("미결제 주문 취소 성공 - 환불 없음")
        void cancelUnpaidOrder_success_noRefund() {
            // given
            Order unpaidOrder = createUnpaidOrder("ORDER-1");
            orderRepository.save(unpaidOrder);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-1",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isSuccess());
            CancelOrderUseCase.OrderCancelled cancelled = result.value();
            assertEquals(new OrderId("ORDER-1"), cancelled.orderId());
            assertEquals(CancelReason.CUSTOMER_REQUEST, cancelled.reason());
            assertTrue(cancelled.refundAmount().isZero()); // 환불 없음

            // verify order is saved as cancelled
            Order savedOrder = orderRepository.findById(new OrderId("ORDER-1")).orElseThrow();
            assertTrue(savedOrder.status() instanceof OrderStatus.Cancelled);
            assertFalse(((OrderStatus.Cancelled) savedOrder.status()).hasRefund());
        }
    }

    @Nested
    @DisplayName("결제 완료 주문 취소")
    class PaidOrderCancellation {

        @Test
        @DisplayName("결제 완료 24시간 이내 취소 성공 - 환불 발생")
        void cancelPaidOrder_within24Hours_success_withRefund() {
            // given
            Order paidOrder = createPaidOrder("ORDER-2", LocalDateTime.now().minusHours(12));
            orderRepository.save(paidOrder);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-2",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isSuccess());
            CancelOrderUseCase.OrderCancelled cancelled = result.value();
            assertEquals(new OrderId("ORDER-2"), cancelled.orderId());
            assertEquals(Money.krw(30000), cancelled.refundAmount()); // 환불 발생

            // verify order is saved as cancelled with refund
            Order savedOrder = orderRepository.findById(new OrderId("ORDER-2")).orElseThrow();
            assertTrue(savedOrder.status() instanceof OrderStatus.Cancelled);
            assertTrue(((OrderStatus.Cancelled) savedOrder.status()).hasRefund());
        }

        @Test
        @DisplayName("결제 완료 24시간 초과 취소 실패")
        void cancelPaidOrder_after24Hours_failure() {
            // given
            Order paidOrder = createPaidOrder("ORDER-3", LocalDateTime.now().minusHours(25));
            orderRepository.save(paidOrder);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-3",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.OrderIssue);
            PlaceOrderError.OrderIssue orderIssue = (PlaceOrderError.OrderIssue) result.error();
            assertTrue(orderIssue.error() instanceof OrderError.CannotCancel);
        }
    }

    @Nested
    @DisplayName("배송 중/완료 주문 취소")
    class ShippingOrderCancellation {

        @Test
        @DisplayName("배송 중 주문 취소 실패")
        void cancelShippingOrder_failure() {
            // given
            Order shippingOrder = createShippingOrder("ORDER-4");
            orderRepository.save(shippingOrder);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-4",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.OrderIssue);
            PlaceOrderError.OrderIssue orderIssue = (PlaceOrderError.OrderIssue) result.error();
            assertTrue(orderIssue.error() instanceof OrderError.CannotCancel);
            assertTrue(orderIssue.error().message().contains("배송 중"));
        }

        @Test
        @DisplayName("배송 완료 주문 취소 실패")
        void cancelDeliveredOrder_failure() {
            // given
            Order deliveredOrder = createDeliveredOrder("ORDER-5");
            orderRepository.save(deliveredOrder);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-5",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.OrderIssue);
            PlaceOrderError.OrderIssue orderIssue = (PlaceOrderError.OrderIssue) result.error();
            assertTrue(orderIssue.error() instanceof OrderError.CannotCancel);
            assertTrue(orderIssue.error().message().contains("배송 완료"));
        }
    }

    @Nested
    @DisplayName("환불 처리")
    class RefundProcessing {

        @Test
        @DisplayName("환불 실패 시 에러 반환")
        void refundFailure_returnsError() {
            // given
            Order paidOrder = createPaidOrder("ORDER-6", LocalDateTime.now().minusHours(12));
            orderRepository.save(paidOrder);
            paymentGateway.setAlwaysFail(true);

            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "ORDER-6",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.PaymentIssue);
        }
    }

    @Nested
    @DisplayName("주문 조회 실패")
    class OrderNotFound {

        @Test
        @DisplayName("존재하지 않는 주문 취소 실패")
        void cancelNonExistentOrder_failure() {
            // given
            CancelOrderUseCase.CancelOrderCommand command = new CancelOrderUseCase.CancelOrderCommand(
                "NON-EXISTENT",
                CancelReason.CUSTOMER_REQUEST
            );

            // when
            Result<CancelOrderUseCase.OrderCancelled, PlaceOrderError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.OrderIssue);
            PlaceOrderError.OrderIssue orderIssue = (PlaceOrderError.OrderIssue) result.error();
            assertTrue(orderIssue.error() instanceof OrderError.NotFound);
        }
    }

    // === 헬퍼 메서드 ===

    private Order createUnpaidOrder(String orderId) {
        return Order.create(
            new OrderId(orderId),
            new CustomerId(1L),
            List.of(new OrderLine(new ProductId("P-1"), new Quantity(2), Money.krw(10000))),
            new ShippingAddress("홍길동", null, null, "서울시 강남구", null),
            Money.ZERO,
            Money.krw(3000),
            LocalDateTime.now().plusDays(1)
        );
    }

    private Order createPaidOrder(String orderId, LocalDateTime paidAt) {
        Order order = createUnpaidOrder(orderId);
        return new Order(
            order.id(),
            order.customerId(),
            order.lines(),
            order.shippingAddress(),
            order.subtotal(),
            order.discount(),
            order.shippingFee(),
            Money.krw(30000),
            new OrderStatus.Paid(paidAt, new PaymentMethod.Points(30000), "TX-12345")
        );
    }

    private Order createShippingOrder(String orderId) {
        Order paidOrder = createPaidOrder(orderId, LocalDateTime.now().minusDays(1));
        return new Order(
            paidOrder.id(),
            paidOrder.customerId(),
            paidOrder.lines(),
            paidOrder.shippingAddress(),
            paidOrder.subtotal(),
            paidOrder.discount(),
            paidOrder.shippingFee(),
            paidOrder.totalAmount(),
            new OrderStatus.Shipping(
                LocalDateTime.now().minusDays(1),
                "TRACK-12345",
                LocalDateTime.now()
            )
        );
    }

    private Order createDeliveredOrder(String orderId) {
        return new Order(
            new OrderId(orderId),
            new CustomerId(1L),
            List.of(new OrderLine(new ProductId("P-1"), new Quantity(2), Money.krw(10000))),
            new ShippingAddress("홍길동", null, null, "서울시 강남구", null),
            Money.krw(20000),
            Money.ZERO,
            Money.krw(3000),
            Money.krw(23000),
            new OrderStatus.Delivered(
                LocalDateTime.now().minusDays(3),
                "TRACK-12345",
                LocalDateTime.now().minusDays(1)
            )
        );
    }

    // === Mock 구현 ===

    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<OrderId, Order> store = new HashMap<>();

        @Override
        public Optional<Order> findById(OrderId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Order> findByCustomerId(CustomerId customerId) {
            return store.values().stream()
                .filter(o -> o.customerId().equals(customerId))
                .toList();
        }

        @Override
        public Order save(Order order) {
            store.put(order.id(), order);
            return order;
        }

        @Override
        public void delete(OrderId id) {
            store.remove(id);
        }
    }

    static class MockPaymentGateway implements PaymentGateway {
        private boolean alwaysFail = false;

        public void setAlwaysFail(boolean alwaysFail) {
            this.alwaysFail = alwaysFail;
        }

        @Override
        public Result<String, PaymentError> charge(Money amount, PaymentMethod method) {
            if (alwaysFail) {
                return Result.failure(new PaymentError.SystemError("결제 실패"));
            }
            return Result.success("TX-" + System.currentTimeMillis());
        }

        @Override
        public Result<String, PaymentError> refund(String transactionId, Money amount) {
            if (alwaysFail) {
                return Result.failure(new PaymentError.SystemError("환불 실패"));
            }
            return Result.success("REFUND-" + System.currentTimeMillis());
        }
    }
}
