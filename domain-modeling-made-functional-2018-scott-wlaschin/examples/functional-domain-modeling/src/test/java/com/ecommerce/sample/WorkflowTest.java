package com.ecommerce.sample;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

class WorkflowTest {

    @Nested
    @DisplayName("기존 Workflow 테스트")
    class LegacyWorkflowTests {

        @Test
        void workflow_returns_order_placed_on_success() {
            Workflow workflow = new Workflow();
            OrderLine line = new OrderLine(
                new ProductId("P-1"),
                new Quantity(2),
                new Money(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Workflow.PlaceOrderCommand cmd = new Workflow.PlaceOrderCommand(
                "1",
                List.of(line),
                "Seoul",
                "COUPON-1"
            );

            Result<OrderPlaced, String> result = workflow.execute(cmd);

            assertTrue(result.isSuccess());
            assertEquals(BigDecimal.valueOf(20000), result.value().totalAmount().amount());
        }

        @Test
        void workflow_returns_failure_on_empty_order() {
            Workflow workflow = new Workflow();
            Workflow.PlaceOrderCommand cmd = new Workflow.PlaceOrderCommand(
                "1",
                List.of(),
                "Seoul",
                "COUPON-1"
            );

            Result<OrderPlaced, String> result = workflow.execute(cmd);

            assertTrue(result.isFailure());
            assertEquals("Empty order", result.error());
        }
    }

    @Nested
    @DisplayName("개선된 Workflow (Validation 사용) 테스트")
    class ValidationWorkflowTests {

        @Test
        void validation_workflow_returns_success() {
            Workflow workflow = new Workflow();
            OrderLine line = new OrderLine(
                new ProductId("P-1"),
                new Quantity(2),
                new Money(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Workflow.PlaceOrderCommand cmd = new Workflow.PlaceOrderCommand(
                "1",
                List.of(line),
                "Seoul",
                "COUPON-1"
            );

            Result<OrderPlaced, List<OrderError>> result = workflow.executeWithValidation(cmd);

            assertTrue(result.isSuccess());
            assertEquals(BigDecimal.valueOf(20000), result.value().totalAmount().amount());
        }

        @Test
        void validation_workflow_collects_all_errors() {
            Workflow workflow = new Workflow();
            Workflow.PlaceOrderCommand cmd = new Workflow.PlaceOrderCommand(
                "invalid-id",  // 에러 1: 잘못된 고객 ID
                List.of(),     // 에러 2: 빈 주문
                "",            // 에러 3: 빈 주소
                ""             // 에러 4: 빈 쿠폰
            );

            Result<OrderPlaced, List<OrderError>> result = workflow.executeWithValidation(cmd);

            assertTrue(result.isFailure());
            List<OrderError> errors = result.error();
            assertEquals(4, errors.size());

            assertTrue(errors.stream().anyMatch(e -> e instanceof OrderError.InvalidCustomerId));
            assertTrue(errors.stream().anyMatch(e -> e instanceof OrderError.EmptyOrder));
            assertTrue(errors.stream().anyMatch(e -> e instanceof OrderError.InvalidShippingAddress));
            assertTrue(errors.stream().anyMatch(e -> e instanceof OrderError.InvalidCouponCode));
        }
    }

    @Nested
    @DisplayName("Sum Type 테스트")
    class SumTypeTests {

        @Test
        void payment_method_credit_card() {
            PaymentMethod card = new PaymentMethod.CreditCard("1234-5678-9012-3456", "12/25", "123");
            assertTrue(card instanceof PaymentMethod.CreditCard);
        }

        @Test
        void payment_method_exhaustive_switch() {
            PaymentMethod method = new PaymentMethod.Points(1000);

            String result = switch (method) {
                case PaymentMethod.CreditCard c -> "Card: " + c.cardNumber();
                case PaymentMethod.BankTransfer b -> "Bank: " + b.bankCode();
                case PaymentMethod.Points p -> "Points: " + p.amount();
                case PaymentMethod.SimplePay s -> "SimplePay: " + s.provider();
            };

            assertEquals("Points: 1000", result);
        }

        @Test
        void order_status_unpaid() {
            OrderStatus status = new OrderStatus.Unpaid(LocalDateTime.now().plusDays(1));
            assertTrue(status instanceof OrderStatus.Unpaid);
        }

        @Test
        void order_status_exhaustive_switch() {
            OrderStatus status = new OrderStatus.Paid(
                LocalDateTime.now(),
                new PaymentMethod.Points(1000),
                "TX-123"
            );

            String result = switch (status) {
                case OrderStatus.Unpaid u -> "미결제";
                case OrderStatus.Paid p -> "결제 완료: " + p.transactionId();
                case OrderStatus.Shipping s -> "배송 중: " + s.trackingNumber();
                case OrderStatus.Delivered d -> "배송 완료";
                case OrderStatus.Cancelled c -> "취소됨: " + c.reason();
            };

            assertEquals("결제 완료: TX-123", result);
        }

        @Test
        void coupon_type_fixed_discount() {
            Money originalPrice = new Money(BigDecimal.valueOf(10000), Currency.KRW);
            CouponType coupon = new CouponType.FixedAmountDiscount(
                new Money(BigDecimal.valueOf(3000), Currency.KRW)
            );

            Money discount = coupon.calculateDiscount(originalPrice);

            assertEquals(BigDecimal.valueOf(3000), discount.amount());
        }

        @Test
        void coupon_type_percentage_discount() {
            Money originalPrice = new Money(BigDecimal.valueOf(10000), Currency.KRW);
            CouponType coupon = new CouponType.PercentageDiscount(10);

            Money discount = coupon.calculateDiscount(originalPrice);

            assertEquals(0, BigDecimal.valueOf(1000).compareTo(discount.amount()));
        }

        @Test
        void coupon_type_fixed_discount_capped_at_original_price() {
            Money originalPrice = new Money(BigDecimal.valueOf(2000), Currency.KRW);
            CouponType coupon = new CouponType.FixedAmountDiscount(
                new Money(BigDecimal.valueOf(5000), Currency.KRW)
            );

            Money discount = coupon.calculateDiscount(originalPrice);

            assertEquals(BigDecimal.valueOf(2000), discount.amount());
        }
    }

    @Nested
    @DisplayName("Domain Error 테스트")
    class DomainErrorTests {

        @Test
        void error_handler_processes_all_error_types() {
            List<OrderError> errors = List.of(
                new OrderError.EmptyOrder(),
                new OrderError.InvalidCustomerId("abc"),
                new OrderError.InvalidShippingAddress(""),
                new OrderError.InvalidCouponCode("INVALID", "만료된 쿠폰"),
                new OrderError.OutOfStock(new ProductId("P-1"), 10, 5),
                new OrderError.PaymentFailed("카드 한도 초과")
            );

            for (OrderError error : errors) {
                String message = OrderErrorHandler.handleError(error);
                assertNotNull(message);
                assertFalse(message.isEmpty());
            }
        }

        @Test
        void combine_errors_creates_single_message() {
            List<OrderError> errors = List.of(
                new OrderError.EmptyOrder(),
                new OrderError.InvalidCustomerId("abc")
            );

            String combined = OrderErrorHandler.combineErrors(errors);

            assertTrue(combined.contains("주문 상품이 없습니다"));
            assertTrue(combined.contains("유효하지 않은 고객 ID"));
        }
    }

    @Nested
    @DisplayName("Result 타입 테스트")
    class ResultTests {

        @Test
        void map_transforms_success_value() {
            Result<Integer, String> result = Result.success(5);
            Result<Integer, String> mapped = result.map(x -> x * 2);

            assertTrue(mapped.isSuccess());
            assertEquals(10, mapped.value());
        }

        @Test
        void map_preserves_failure() {
            Result<Integer, String> result = Result.failure("error");
            Result<Integer, String> mapped = result.map(x -> x * 2);

            assertTrue(mapped.isFailure());
            assertEquals("error", mapped.error());
        }

        @Test
        void flatMap_chains_successful_operations() {
            Result<Integer, String> result = Result.success(5);
            Result<Integer, String> chained = result.flatMap(x ->
                x > 0 ? Result.success(x * 2) : Result.failure("negative")
            );

            assertTrue(chained.isSuccess());
            assertEquals(10, chained.value());
        }

        @Test
        void flatMap_short_circuits_on_failure() {
            Result<Integer, String> result = Result.failure("initial error");
            Result<Integer, String> chained = result.flatMap(x -> Result.success(x * 2));

            assertTrue(chained.isFailure());
            assertEquals("initial error", chained.error());
        }

        @Test
        void mapError_transforms_error() {
            Result<Integer, String> result = Result.failure("error");
            Result<Integer, Integer> mapped = result.mapError(String::length);

            assertTrue(mapped.isFailure());
            assertEquals(5, mapped.error());
        }
    }

    @Nested
    @DisplayName("Validation 타입 테스트")
    class ValidationTests {

        @Test
        void combine_two_valid_values() {
            Validation<Integer, List<String>> v1 = Validation.valid(1);
            Validation<Integer, List<String>> v2 = Validation.valid(2);

            Validation<Integer, List<String>> result = Validation.combine(v1, v2, Integer::sum);

            assertTrue(result instanceof Valid);
            assertEquals(3, ((Valid<Integer, List<String>>) result).value());
        }

        @Test
        void combine_collects_all_errors() {
            Validation<Integer, List<String>> v1 = Validation.invalidOne("error1");
            Validation<Integer, List<String>> v2 = Validation.invalidOne("error2");

            Validation<Integer, List<String>> result = Validation.combine(v1, v2, Integer::sum);

            assertTrue(result instanceof Invalid);
            List<String> errors = ((Invalid<Integer, List<String>>) result).errors();
            assertEquals(2, errors.size());
            assertTrue(errors.contains("error1"));
            assertTrue(errors.contains("error2"));
        }

        @Test
        void combine4_collects_all_errors() {
            Validation<Integer, List<String>> v1 = Validation.invalidOne("e1");
            Validation<Integer, List<String>> v2 = Validation.invalidOne("e2");
            Validation<Integer, List<String>> v3 = Validation.invalidOne("e3");
            Validation<Integer, List<String>> v4 = Validation.invalidOne("e4");

            Validation<Integer, List<String>> result = Validation.combine4(
                v1, v2, v3, v4,
                (a, b, c, d) -> a + b + c + d
            );

            assertTrue(result instanceof Invalid);
            List<String> errors = ((Invalid<Integer, List<String>>) result).errors();
            assertEquals(4, errors.size());
        }

        @Test
        void fold_converts_to_result() {
            Validation<Integer, List<String>> valid = Validation.valid(42);
            Validation<Integer, List<String>> invalid = Validation.invalidOne("error");

            Result<Integer, List<String>> successResult = valid.fold(Result::success, Result::failure);
            Result<Integer, List<String>> failureResult = invalid.fold(Result::success, Result::failure);

            assertTrue(successResult.isSuccess());
            assertEquals(42, successResult.value());
            assertTrue(failureResult.isFailure());
            assertEquals(1, failureResult.error().size());
        }
    }
}
