package com.ecommerce.domain.payment;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

@DisplayName("결제 도메인 테스트")
class PaymentDomainTest {

    @Nested
    @DisplayName("PaymentMethod 테스트")
    class PaymentMethodTests {

        @Test
        @DisplayName("신용카드 결제 수단 생성")
        void creditCard() {
            PaymentMethod.CreditCard card = new PaymentMethod.CreditCard(
                "1234-5678-9012-3456",
                "12/25",
                "123"
            );

            assertEquals("1234-****-****-3456", card.maskedCardNumber());
            assertFalse(card.isInstallment());
        }

        @Test
        @DisplayName("신용카드 할부 결제")
        void creditCard_installment() {
            PaymentMethod.CreditCard card = new PaymentMethod.CreditCard(
                "1234-5678-9012-3456",
                "12/25",
                "123",
                6
            );

            assertTrue(card.isInstallment());
            assertEquals(6, card.installmentMonths());
        }

        @Test
        @DisplayName("계좌이체 결제 수단 생성")
        void bankTransfer() {
            PaymentMethod.BankTransfer transfer = new PaymentMethod.BankTransfer(
                "004",
                "123456789012",
                "홍길동"
            );

            assertEquals("123****012", transfer.maskedAccountNumber());
        }

        @Test
        @DisplayName("포인트 결제 수단 생성")
        void points() {
            PaymentMethod.Points points = new PaymentMethod.Points(10000);
            assertEquals(10000, points.amount());
        }

        @Test
        @DisplayName("간편결제 수단 생성")
        void simplePay() {
            PaymentMethod.SimplePay simplePay = new PaymentMethod.SimplePay(
                SimplePayProvider.KAKAO,
                "token-123"
            );

            assertEquals(SimplePayProvider.KAKAO, simplePay.provider());
            assertEquals("카카오페이", simplePay.provider().displayName());
        }

        @Test
        @DisplayName("결제 수단 유효성 검사")
        void validation() {
            assertThrows(IllegalArgumentException.class, () ->
                new PaymentMethod.CreditCard("", "12/25", "123"));

            assertThrows(IllegalArgumentException.class, () ->
                new PaymentMethod.CreditCard("1234", "12/25", "12")); // CVC 2자리

            assertThrows(IllegalArgumentException.class, () ->
                new PaymentMethod.Points(-100));

            assertThrows(IllegalArgumentException.class, () ->
                new PaymentMethod.BankTransfer("004", "", "홍길동"));
        }

        @Test
        @DisplayName("결제 수단별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            PaymentMethod method = new PaymentMethod.SimplePay(
                SimplePayProvider.TOSS,
                "token"
            );

            String result = switch (method) {
                case PaymentMethod.CreditCard c -> "카드: " + c.maskedCardNumber();
                case PaymentMethod.BankTransfer b -> "계좌이체: " + b.bankCode();
                case PaymentMethod.Points p -> "포인트: " + p.amount();
                case PaymentMethod.SimplePay s -> "간편결제: " + s.provider().displayName();
            };

            assertEquals("간편결제: 토스", result);
        }
    }

    @Nested
    @DisplayName("PaymentResult 테스트")
    class PaymentResultTests {

        @Test
        @DisplayName("결제 성공 결과")
        void success() {
            PaymentResult.Success success = new PaymentResult.Success(
                "TX-123",
                Money.krw(10000),
                new PaymentMethod.Points(10000)
            );

            assertEquals("TX-123", success.transactionId());
            assertNotNull(success.processedAt());
        }

        @Test
        @DisplayName("결제 실패 결과")
        void failure() {
            PaymentResult.Failure failure = new PaymentResult.Failure(
                new PaymentError.InsufficientFunds(Money.krw(10000), Money.krw(5000))
            );

            assertTrue(failure.error() instanceof PaymentError.InsufficientFunds);
            assertNotNull(failure.failedAt());
        }

        @Test
        @DisplayName("결제 결과별 패턴 매칭")
        void patternMatching() {
            PaymentResult result = new PaymentResult.Failure(
                new PaymentError.CardExpired("12/23")
            );

            String message = switch (result) {
                case PaymentResult.Success s -> "결제 완료: " + s.transactionId();
                case PaymentResult.Failure f -> "결제 실패: " + f.error().message();
            };

            assertTrue(message.contains("만료"));
        }
    }

    @Nested
    @DisplayName("PaymentError 테스트")
    class PaymentErrorTests {

        @Test
        @DisplayName("잔액 부족 에러")
        void insufficientFunds() {
            PaymentError error = new PaymentError.InsufficientFunds(
                Money.krw(10000),
                Money.krw(5000)
            );

            assertEquals("INSUFFICIENT_FUNDS", error.code());
            assertTrue(error.message().contains("부족"));
        }

        @Test
        @DisplayName("카드 만료 에러")
        void cardExpired() {
            PaymentError error = new PaymentError.CardExpired("12/23");

            assertEquals("CARD_EXPIRED", error.code());
            assertTrue(error.message().contains("만료"));
        }

        @Test
        @DisplayName("카드 거절 에러")
        void cardDeclined() {
            PaymentError error = new PaymentError.CardDeclined("한도 초과");

            assertEquals("CARD_DECLINED", error.code());
            assertTrue(error.message().contains("거절"));
        }

        @Test
        @DisplayName("시스템 에러")
        void systemError() {
            PaymentError error = new PaymentError.SystemError("내부 오류");

            assertEquals("SYSTEM_ERROR", error.code());
            // 외부에 보여주는 메시지는 일반적인 안내
            assertTrue(error.message().contains("시스템"));
        }

        @Test
        @DisplayName("에러 타입별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            PaymentError error = new PaymentError.PaymentTimeout();

            String result = switch (error) {
                case PaymentError.InsufficientFunds e -> "잔액 부족";
                case PaymentError.CardExpired e -> "카드 만료";
                case PaymentError.CardDeclined e -> "카드 거절";
                case PaymentError.InvalidPaymentMethod e -> "잘못된 결제 수단";
                case PaymentError.PaymentTimeout e -> "타임아웃";
                case PaymentError.SystemError e -> "시스템 오류";
            };

            assertEquals("타임아웃", result);
        }
    }

    @Nested
    @DisplayName("TransactionId 테스트")
    class TransactionIdTests {

        @Test
        @DisplayName("거래 ID 생성")
        void generate() {
            TransactionId id = TransactionId.generate();
            assertTrue(id.value().startsWith("TXN-"));
        }

        @Test
        @DisplayName("거래 ID 유효성 검사")
        void validation() {
            assertThrows(IllegalArgumentException.class, () -> new TransactionId(""));
            assertThrows(IllegalArgumentException.class, () -> new TransactionId(null));
            assertDoesNotThrow(() -> new TransactionId("TX-123"));
        }
    }
}
