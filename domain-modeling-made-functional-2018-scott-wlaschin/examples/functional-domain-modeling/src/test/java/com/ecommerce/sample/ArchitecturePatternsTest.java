package com.ecommerce.sample;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Chapter 8-9: 아키텍처 패턴 테스트")
class ArchitecturePatternsTest {

    @Nested
    @DisplayName("Ch 8.2: DTO ↔ Domain 변환 테스트")
    class DtoMappingTests {

        @Test
        @DisplayName("DTO → Domain 변환 성공")
        void toDomain_success() {
            OrderDto dto = new OrderDto();
            dto.orderId = "ORD-123";
            dto.customerId = "1";
            dto.amount = BigDecimal.valueOf(10000);
            dto.currencyCode = "KRW";

            Result<OrderSummary, String> result = OrderMapper.toDomain(dto);

            assertTrue(result.isSuccess());
            assertEquals("ORD-123", result.value().orderId().value());
            assertEquals(1L, result.value().customerId().value());
            assertEquals(BigDecimal.valueOf(10000), result.value().amount().amount());
        }

        @Test
        @DisplayName("DTO → Domain 변환 실패 (잘못된 고객 ID)")
        void toDomain_failure_invalidCustomerId() {
            OrderDto dto = new OrderDto();
            dto.orderId = "ORD-123";
            dto.customerId = "invalid-id";
            dto.amount = BigDecimal.valueOf(10000);
            dto.currencyCode = "KRW";

            Result<OrderSummary, String> result = OrderMapper.toDomain(dto);

            assertTrue(result.isFailure());
            assertTrue(result.error().contains("변환 실패"));
        }

        @Test
        @DisplayName("Domain → DTO 변환")
        void toDto_success() {
            OrderSummary summary = new OrderSummary(
                new OrderId("ORD-456"),
                new CustomerId(99L),
                new Money(BigDecimal.valueOf(50000), Currency.KRW)
            );

            OrderDto dto = OrderMapper.toDto(summary);

            assertEquals("ORD-456", dto.orderId);
            assertEquals("99", dto.customerId);
            assertEquals(BigDecimal.valueOf(50000), dto.amount);
            assertEquals("KRW", dto.currencyCode);
        }

        @Test
        @DisplayName("DTO → Domain → DTO 라운드트립 보존")
        void roundTrip_preservesData() {
            OrderDto original = new OrderDto();
            original.orderId = "ORD-789";
            original.customerId = "42";
            original.amount = BigDecimal.valueOf(25000);
            original.currencyCode = "USD";

            Result<OrderSummary, String> domainResult = OrderMapper.toDomain(original);
            assertTrue(domainResult.isSuccess());

            OrderDto converted = OrderMapper.toDto(domainResult.value());

            assertEquals(original.orderId, converted.orderId);
            assertEquals(original.customerId, converted.customerId);
            assertEquals(original.amount, converted.amount);
            assertEquals(original.currencyCode, converted.currencyCode);
        }
    }

    @Nested
    @DisplayName("Ch 8.3: Anti-Corruption Layer 테스트")
    class AclTests {

        @Test
        @DisplayName("ACL: 외부 성공 응답 → 도메인 타입 변환")
        void translateResponse_success() {
            ExternalPaymentClient mockClient = (amount, method) ->
                new ExternalPaymentResponse("0000", "성공", "TX-12345");

            PaymentGatewayAdapter adapter = new PaymentGatewayAdapter(mockClient);
            Money amount = new Money(BigDecimal.valueOf(10000), Currency.KRW);
            PaymentMethod method = new PaymentMethod.Points(1000);

            Result<PaymentApproval, PaymentError> result = adapter.processPayment(amount, method);

            assertTrue(result.isSuccess());
            assertEquals("TX-12345", result.value().transactionId());
        }

        @Test
        @DisplayName("ACL: 외부 에러 코드 → 도메인 에러 타입 변환 (잔액 부족)")
        void translateResponse_failure_insufficientFunds() {
            ExternalPaymentClient mockClient = (amount, method) ->
                new ExternalPaymentResponse("1001", "잔액 부족", null);

            PaymentGatewayAdapter adapter = new PaymentGatewayAdapter(mockClient);
            Money amount = new Money(BigDecimal.valueOf(10000), Currency.KRW);
            PaymentMethod method = new PaymentMethod.Points(1000);

            Result<PaymentApproval, PaymentError> result = adapter.processPayment(amount, method);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PaymentError.InsufficientFunds);
        }

        @Test
        @DisplayName("ACL: 외부 API 예외 → 도메인 에러 타입 변환")
        void translateResponse_exception_toSystemError() {
            ExternalPaymentClient mockClient = (amount, method) -> {
                throw new ExternalApiException("네트워크 오류");
            };

            PaymentGatewayAdapter adapter = new PaymentGatewayAdapter(mockClient);
            Money amount = new Money(BigDecimal.valueOf(10000), Currency.KRW);
            PaymentMethod method = new PaymentMethod.Points(1000);

            Result<PaymentApproval, PaymentError> result = adapter.processPayment(amount, method);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PaymentError.SystemError);
            assertEquals("네트워크 오류", ((PaymentError.SystemError) result.error()).message());
        }
    }

    @Nested
    @DisplayName("Ch 9.2: 함수형 의존성 주입 테스트")
    class FunctionalDiTests {

        @Test
        @DisplayName("함수 파라미터로 의존성 주입")
        void convertPrice_withInjectedRate() {
            PriceService service = new PriceService();
            Money usd = new Money(BigDecimal.valueOf(100), Currency.USD);

            GetExchangeRate mockRate = (from, to) -> BigDecimal.valueOf(1300);

            Money krw = service.convertPrice(usd, Currency.KRW, mockRate);

            assertEquals(BigDecimal.valueOf(130000), krw.amount());
            assertEquals(Currency.KRW, krw.currency());
        }

        @Test
        @DisplayName("부분 적용: 환율 변환기 생성")
        void currencyConverter_partialApplication() {
            GetExchangeRate mockRate = (from, to) -> {
                if (from == Currency.USD && to == Currency.KRW) {
                    return BigDecimal.valueOf(1300);
                }
                return BigDecimal.ONE;
            };

            CurrencyConverter converter = new CurrencyConverter(mockRate);
            Function<Money, Money> toKrw = converter.toKrw();

            Money usd = new Money(BigDecimal.valueOf(50), Currency.USD);
            Money krw = toKrw.apply(usd);

            assertEquals(BigDecimal.valueOf(65000), krw.amount());
        }
    }

    @Nested
    @DisplayName("Ch 9.4: Functional Core (순수 도메인 서비스) 테스트")
    class FunctionalCoreTests {

        private final OrderDomainService service = new OrderDomainService();

        @Test
        @DisplayName("순수 함수: 가격 계산 (쿠폰 없음)")
        void calculatePrice_noCoupon() {
            ValidatedOrder order = new ValidatedOrder(
                new CustomerId(1L),
                List.of(
                    new OrderLine(new ProductId("P-1"), new Quantity(2),
                        new Money(BigDecimal.valueOf(10000), Currency.KRW))
                ),
                new ShippingAddress("Seoul"),
                new CouponCode("NONE")
            );

            PricedOrderFull result = service.calculatePrice(order);

            assertEquals(BigDecimal.valueOf(20000), result.subtotal().amount());
            assertEquals(BigDecimal.ZERO, result.discount().amount());
            assertEquals(BigDecimal.valueOf(20000), result.totalAmount().amount());
        }

        @Test
        @DisplayName("순수 함수: 가격 계산 (정률 쿠폰 적용)")
        void calculatePrice_withPercentageCoupon() {
            ValidatedOrder order = new ValidatedOrder(
                new CustomerId(1L),
                List.of(
                    new OrderLine(new ProductId("P-1"), new Quantity(1),
                        new Money(BigDecimal.valueOf(10000), Currency.KRW))
                ),
                new ShippingAddress("Seoul"),
                new CouponCode("DISCOUNT10")
            );
            CouponType coupon = new CouponType.PercentageDiscount(10);

            PricedOrderFull result = service.calculatePrice(order, coupon);

            assertEquals(BigDecimal.valueOf(10000), result.subtotal().amount());
            assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.discount().amount()));
            assertEquals(0, BigDecimal.valueOf(9000).compareTo(result.totalAmount().amount()));
        }

        @Test
        @DisplayName("순수 함수: 가격 계산 (정액 쿠폰 적용)")
        void calculatePrice_withFixedCoupon() {
            ValidatedOrder order = new ValidatedOrder(
                new CustomerId(1L),
                List.of(
                    new OrderLine(new ProductId("P-1"), new Quantity(1),
                        new Money(BigDecimal.valueOf(10000), Currency.KRW))
                ),
                new ShippingAddress("Seoul"),
                new CouponCode("FIXED3000")
            );
            CouponType coupon = new CouponType.FixedAmountDiscount(
                new Money(BigDecimal.valueOf(3000), Currency.KRW)
            );

            PricedOrderFull result = service.calculatePrice(order, coupon);

            assertEquals(BigDecimal.valueOf(10000), result.subtotal().amount());
            assertEquals(BigDecimal.valueOf(3000), result.discount().amount());
            assertEquals(BigDecimal.valueOf(7000), result.totalAmount().amount());
        }

        @Test
        @DisplayName("순수 함수: 취소 가능 여부 (미결제)")
        void canCancel_unpaid_returnsTrue() {
            OrderStatus status = new OrderStatus.Unpaid(LocalDateTime.now().plusDays(1));
            assertTrue(service.canCancel(status));
        }

        @Test
        @DisplayName("순수 함수: 취소 가능 여부 (결제 완료)")
        void canCancel_paid_returnsTrue() {
            OrderStatus status = new OrderStatus.Paid(
                LocalDateTime.now(),
                new PaymentMethod.Points(1000),
                "TX-123"
            );
            assertTrue(service.canCancel(status));
        }

        @Test
        @DisplayName("순수 함수: 취소 불가 (배송 중)")
        void canCancel_shipping_returnsFalse() {
            OrderStatus status = new OrderStatus.Shipping(
                LocalDateTime.now(),
                "TRACK-123",
                LocalDateTime.now()
            );
            assertFalse(service.canCancel(status));
        }

        @Test
        @DisplayName("순수 함수: 취소 불가 (배송 완료)")
        void canCancel_delivered_returnsFalse() {
            OrderStatus status = new OrderStatus.Delivered(
                LocalDateTime.now(),
                "TRACK-123",
                LocalDateTime.now()
            );
            assertFalse(service.canCancel(status));
        }
    }

    @Nested
    @DisplayName("Ch 9: Imperative Shell (Use Case) 테스트")
    class ImperativeShellTests {

        @Test
        @DisplayName("Use Case: 주문 처리 성공")
        void placeOrder_success() {
            OrderRepository mockOrderRepo = new OrderRepository() {
                @Override public Optional<Order> findById(OrderId id) { return Optional.empty(); }
                @Override public Order save(Order order) { return order; }
                @Override public void delete(OrderId id) {}
            };

            ExternalPaymentClient mockPaymentClient = (amount, method) ->
                new ExternalPaymentResponse("0000", "성공", "TX-SUCCESS");

            CouponRepository mockCouponRepo = code -> Optional.empty();

            PlaceOrderUseCase useCase = new PlaceOrderUseCase(
                mockOrderRepo,
                new PaymentGatewayAdapter(mockPaymentClient),
                new OrderDomainService(),
                mockCouponRepo
            );

            ValidatedOrder order = new ValidatedOrder(
                new CustomerId(1L),
                List.of(new OrderLine(
                    new ProductId("P-1"),
                    new Quantity(1),
                    new Money(BigDecimal.valueOf(10000), Currency.KRW)
                )),
                new ShippingAddress("Seoul"),
                new CouponCode("NONE")
            );
            PaymentMethod method = new PaymentMethod.Points(10000);

            Result<OrderPlaced, OrderError> result = useCase.execute(order, method);

            assertTrue(result.isSuccess());
            assertNotNull(result.value().orderId());
        }

        @Test
        @DisplayName("Use Case: 결제 실패 시 에러 반환")
        void placeOrder_paymentFailed() {
            OrderRepository mockOrderRepo = new OrderRepository() {
                @Override public Optional<Order> findById(OrderId id) { return Optional.empty(); }
                @Override public Order save(Order order) { return order; }
                @Override public void delete(OrderId id) {}
            };

            ExternalPaymentClient mockPaymentClient = (amount, method) ->
                new ExternalPaymentResponse("1001", "잔액 부족", null);

            CouponRepository mockCouponRepo = code -> Optional.empty();

            PlaceOrderUseCase useCase = new PlaceOrderUseCase(
                mockOrderRepo,
                new PaymentGatewayAdapter(mockPaymentClient),
                new OrderDomainService(),
                mockCouponRepo
            );

            ValidatedOrder order = new ValidatedOrder(
                new CustomerId(1L),
                List.of(new OrderLine(
                    new ProductId("P-1"),
                    new Quantity(1),
                    new Money(BigDecimal.valueOf(10000), Currency.KRW)
                )),
                new ShippingAddress("Seoul"),
                new CouponCode("NONE")
            );
            PaymentMethod method = new PaymentMethod.Points(10000);

            Result<OrderPlaced, OrderError> result = useCase.execute(order, method);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof OrderError.PaymentFailed);
        }
    }

    @Nested
    @DisplayName("Ch 8.1: Persistence Ignorance 테스트")
    class PersistenceIgnoranceTests {

        @Test
        @DisplayName("Order 도메인 모델은 JPA 어노테이션 없이 순수 비즈니스 로직만 포함")
        void order_domainModel_pureBusinessLogic() {
            Order order = new Order(
                new OrderId("ORD-1"),
                new CustomerId(1L),
                List.of(new OrderLine(
                    new ProductId("P-1"),
                    new Quantity(1),
                    new Money(BigDecimal.valueOf(10000), Currency.KRW)
                )),
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new OrderStatus.Unpaid(LocalDateTime.now().plusDays(1))
            );

            Order cancelled = order.cancel(CancelReason.CUSTOMER_REQUEST);

            assertTrue(cancelled.status() instanceof OrderStatus.Cancelled);
            assertEquals(CancelReason.CUSTOMER_REQUEST,
                ((OrderStatus.Cancelled) cancelled.status()).reason());
        }

        @Test
        @DisplayName("취소 불가 상태에서 cancel() 호출 시 예외")
        void order_cancel_throwsOnInvalidState() {
            Order order = new Order(
                new OrderId("ORD-1"),
                new CustomerId(1L),
                List.of(),
                new Money(BigDecimal.valueOf(10000), Currency.KRW),
                new OrderStatus.Shipping(
                    LocalDateTime.now(),
                    "TRACK-123",
                    LocalDateTime.now()
                )
            );

            assertThrows(IllegalStateException.class, () ->
                order.cancel(CancelReason.CUSTOMER_REQUEST)
            );
        }
    }
}
