package com.ecommerce.domain.order;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.domain.payment.PaymentMethod;
import com.ecommerce.domain.product.ProductId;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("주문 도메인 테스트")
class OrderDomainTest {

    @Nested
    @DisplayName("OrderStatus 테스트")
    class OrderStatusTests {

        @Test
        @DisplayName("Unpaid 상태 - 결제 기한 내")
        void unpaid_withinDeadline() {
            OrderStatus.Unpaid unpaid = new OrderStatus.Unpaid(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
            );

            assertFalse(unpaid.isExpired());
        }

        @Test
        @DisplayName("Unpaid 상태 - 결제 기한 초과")
        void unpaid_expired() {
            OrderStatus.Unpaid unpaid = new OrderStatus.Unpaid(
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
            );

            assertTrue(unpaid.isExpired());
        }

        @Test
        @DisplayName("Paid 상태 생성")
        void paid() {
            OrderStatus.Paid paid = new OrderStatus.Paid(
                LocalDateTime.now(),
                new PaymentMethod.Points(10000),
                "TX-123"
            );

            assertEquals("TX-123", paid.transactionId());
        }

        @Test
        @DisplayName("Shipping 상태 생성")
        void shipping() {
            OrderStatus.Shipping shipping = new OrderStatus.Shipping(
                LocalDateTime.now(),
                "TRACK-123",
                LocalDateTime.now()
            );

            assertEquals("TRACK-123", shipping.trackingNumber());
        }

        @Test
        @DisplayName("Cancelled 상태 - 환불 없음")
        void cancelled_noRefund() {
            OrderStatus.Cancelled cancelled = new OrderStatus.Cancelled(
                LocalDateTime.now(),
                CancelReason.CUSTOMER_REQUEST
            );

            assertFalse(cancelled.hasRefund());
        }

        @Test
        @DisplayName("Cancelled 상태 - 환불 있음")
        void cancelled_withRefund() {
            OrderStatus.Cancelled cancelled = new OrderStatus.Cancelled(
                LocalDateTime.now(),
                CancelReason.CUSTOMER_REQUEST,
                new RefundInfo("TX-123", Money.krw(10000), LocalDateTime.now())
            );

            assertTrue(cancelled.hasRefund());
            assertEquals("TX-123", cancelled.refundInfo().originalTransactionId());
        }

        @Test
        @DisplayName("상태별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            OrderStatus status = new OrderStatus.Shipping(
                LocalDateTime.now(),
                "TRACK-123",
                LocalDateTime.now()
            );

            String result = switch (status) {
                case OrderStatus.Unpaid u -> "미결제";
                case OrderStatus.Paid p -> "결제 완료: " + p.transactionId();
                case OrderStatus.Shipping s -> "배송 중: " + s.trackingNumber();
                case OrderStatus.Delivered d -> "배송 완료";
                case OrderStatus.Cancelled c -> "취소됨: " + c.reason();
            };

            assertEquals("배송 중: TRACK-123", result);
        }
    }

    @Nested
    @DisplayName("Order 테스트")
    class OrderTests {

        private Order createUnpaidOrder() {
            return Order.create(
                OrderId.generate(),
                new CustomerId(1L),
                List.of(new OrderLine(
                    new ProductId("P-1"),
                    "테스트 상품",
                    new Quantity(2),
                    Money.krw(10000)
                )),
                ShippingAddress.simple("서울시 강남구"),
                Money.ZERO,
                Money.krw(3000),
                LocalDateTime.now().plusDays(1)
            );
        }

        @Test
        @DisplayName("주문 생성")
        void createOrder() {
            Order order = createUnpaidOrder();

            assertTrue(order.status() instanceof OrderStatus.Unpaid);
            assertEquals(BigDecimal.valueOf(23000), order.totalAmount().amount()); // 20,000 + 3,000
        }

        @Test
        @DisplayName("결제 성공")
        void pay_success() {
            Order order = createUnpaidOrder();
            PaymentMethod method = new PaymentMethod.Points(23000);

            Result<Order, OrderError> result = order.pay(method, "TX-123");

            assertTrue(result.isSuccess());
            assertTrue(result.value().isPaid());
        }

        @Test
        @DisplayName("결제 실패 - 결제 기한 초과")
        void pay_deadline_exceeded() {
            Order order = Order.create(
                OrderId.generate(),
                new CustomerId(1L),
                List.of(new OrderLine(
                    new ProductId("P-1"),
                    "테스트 상품",
                    new Quantity(1),
                    Money.krw(10000)
                )),
                ShippingAddress.simple("서울시 강남구"),
                Money.ZERO,
                Money.krw(3000),
                LocalDateTime.now().minusDays(1)  // 이미 만료
            );

            Result<Order, OrderError> result = order.pay(
                new PaymentMethod.Points(13000),
                "TX-123"
            );

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof OrderError.PaymentDeadlineExceeded);
        }

        @Test
        @DisplayName("배송 시작")
        void startShipping() {
            Order order = createUnpaidOrder();
            Order paidOrder = order.pay(new PaymentMethod.Points(23000), "TX-123").value();

            Result<Order, OrderError> result = paidOrder.startShipping("TRACK-456");

            assertTrue(result.isSuccess());
            assertTrue(result.value().isShipping());
        }

        @Test
        @DisplayName("배송 완료")
        void completeDelivery() {
            Order order = createUnpaidOrder();
            Order paidOrder = order.pay(new PaymentMethod.Points(23000), "TX-123").value();
            Order shippingOrder = paidOrder.startShipping("TRACK-456").value();

            Result<Order, OrderError> result = shippingOrder.completeDelivery();

            assertTrue(result.isSuccess());
            assertTrue(result.value().isDelivered());
        }

        @Test
        @DisplayName("미결제 주문 취소")
        void cancel_unpaid() {
            Order order = createUnpaidOrder();

            Result<Order, OrderError> result = order.cancel(CancelReason.CUSTOMER_REQUEST);

            assertTrue(result.isSuccess());
            assertTrue(result.value().isCancelled());

            OrderStatus.Cancelled cancelled = (OrderStatus.Cancelled) result.value().status();
            assertFalse(cancelled.hasRefund()); // 환불 없음
        }

        @Test
        @DisplayName("결제 완료 주문 취소 (24시간 내)")
        void cancel_paid_within24hours() {
            Order order = createUnpaidOrder();
            Order paidOrder = order.pay(new PaymentMethod.Points(23000), "TX-123").value();

            Result<Order, OrderError> result = paidOrder.cancel(CancelReason.CUSTOMER_REQUEST);

            assertTrue(result.isSuccess());
            assertTrue(result.value().isCancelled());

            OrderStatus.Cancelled cancelled = (OrderStatus.Cancelled) result.value().status();
            assertTrue(cancelled.hasRefund()); // 환불 있음
        }

        @Test
        @DisplayName("배송 중 주문 취소 불가")
        void cancel_shipping_notAllowed() {
            Order order = createUnpaidOrder();
            Order paidOrder = order.pay(new PaymentMethod.Points(23000), "TX-123").value();
            Order shippingOrder = paidOrder.startShipping("TRACK-456").value();

            Result<Order, OrderError> result = shippingOrder.cancel(CancelReason.CUSTOMER_REQUEST);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof OrderError.CannotCancel);
        }

        @Test
        @DisplayName("취소 가능 여부 확인")
        void canCancel() {
            Order unpaidOrder = createUnpaidOrder();
            assertTrue(unpaidOrder.canCancel());

            Order paidOrder = unpaidOrder.pay(new PaymentMethod.Points(23000), "TX-123").value();
            assertTrue(paidOrder.canCancel()); // 24시간 내

            Order shippingOrder = paidOrder.startShipping("TRACK-456").value();
            assertFalse(shippingOrder.canCancel());

            Order deliveredOrder = shippingOrder.completeDelivery().value();
            assertFalse(deliveredOrder.canCancel());
        }
    }

    @Nested
    @DisplayName("OrderLine 테스트")
    class OrderLineTests {

        @Test
        @DisplayName("주문 라인 소계 계산")
        void subtotal() {
            OrderLine line = new OrderLine(
                new ProductId("P-1"),
                "테스트 상품",
                new Quantity(3),
                Money.krw(10000)
            );

            assertEquals(BigDecimal.valueOf(30000), line.subtotal().amount());
        }
    }

    @Nested
    @DisplayName("Value Object 테스트")
    class ValueObjectTests {

        @Test
        @DisplayName("OrderId 생성")
        void orderId_generate() {
            OrderId id = OrderId.generate();
            assertTrue(id.value().startsWith("ORD-"));
        }

        @Test
        @DisplayName("CustomerId 유효성 검사")
        void customerId_validation() {
            assertThrows(IllegalArgumentException.class, () -> new CustomerId(0));
            assertThrows(IllegalArgumentException.class, () -> new CustomerId(-1));
            assertDoesNotThrow(() -> new CustomerId(1));
        }

        @Test
        @DisplayName("Quantity 유효성 검사")
        void quantity_validation() {
            assertThrows(IllegalArgumentException.class, () -> new Quantity(0));
            assertThrows(IllegalArgumentException.class, () -> new Quantity(-1));
            assertDoesNotThrow(() -> new Quantity(1));
        }

        @Test
        @DisplayName("ShippingAddress 간단 생성")
        void shippingAddress_simple() {
            ShippingAddress address = ShippingAddress.simple("서울시 강남구");
            assertEquals("서울시 강남구", address.address());
            assertEquals("수령인", address.recipient());
        }

        @Test
        @DisplayName("ShippingAddress 전체 주소")
        void shippingAddress_fullAddress() {
            ShippingAddress address = new ShippingAddress(
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 101호"
            );

            assertEquals("서울시 강남구 101동 101호", address.fullAddress());
        }
    }
}
