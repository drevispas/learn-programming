package com.travel.domain.payment;

import com.travel.domain.booking.BookingId;
import com.travel.domain.member.MemberId;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Payment 멱등성 테스트
 */
@DisplayName("Payment - 멱등성 테스트")
class PaymentIdempotencyTest {

    @Test
    @DisplayName("IdempotencyKey로 결제를 고유하게 식별한다")
    void idempotency_key_identifies_payment() {
        // Given
        IdempotencyKey key = IdempotencyKey.generate();

        Payment payment = Payment.create(
                BookingId.generate(),
                MemberId.generate(),
                Money.krw(100000),
                new PaymentMethod.CreditCard(
                        "1234-****-****-5678",
                        PaymentMethod.CreditCard.CardCompany.SHINHAN,
                        0
                ),
                key
        );

        // Then
        assertEquals(key, payment.idempotencyKey());
    }

    @Test
    @DisplayName("동일한 IdempotencyKey 문자열은 같은 작업을 식별한다")
    void same_key_string_identifies_same_operation() {
        // Given
        String keyString = "unique-payment-key-12345";
        IdempotencyKey key1 = IdempotencyKey.from(keyString);
        IdempotencyKey key2 = IdempotencyKey.from(keyString);

        // Then
        assertEquals(key1.key(), key2.key());
    }

    @Test
    @DisplayName("IdempotencyKey는 만료될 수 있다")
    void idempotency_key_can_expire() {
        // Given: 이미 만료된 키
        IdempotencyKey expiredKey = new IdempotencyKey(
                "expired-key",
                Instant.now().minusSeconds(3600 * 25), // 25시간 전 생성
                Instant.now().minusSeconds(3600)       // 1시간 전 만료
        );

        // Then
        assertTrue(expiredKey.isExpired());
        assertFalse(expiredKey.isValid());
    }

    @Test
    @DisplayName("새로 생성된 IdempotencyKey는 유효하다")
    void new_idempotency_key_is_valid() {
        // Given
        IdempotencyKey key = IdempotencyKey.generate();

        // Then
        assertFalse(key.isExpired());
        assertTrue(key.isValid());
    }

    @Test
    @DisplayName("Payment 상태 전이 - Pending → Completed")
    void payment_state_transition_to_completed() {
        // Given
        Payment payment = createPendingPayment();

        // When
        Payment completed = payment.complete("TXN-123456", "APPR-789");

        // Then
        assertInstanceOf(Payment.PaymentStatus.Completed.class, completed.status());
        assertTrue(completed.isCompleted());
    }

    @Test
    @DisplayName("Payment 상태 전이 - Pending → Failed")
    void payment_state_transition_to_failed() {
        // Given
        Payment payment = createPendingPayment();

        // When
        Payment failed = payment.fail("잔액 부족");

        // Then
        assertInstanceOf(Payment.PaymentStatus.Failed.class, failed.status());
        assertTrue(failed.isFailed());
    }

    @Test
    @DisplayName("원본 Payment는 상태 전이 후에도 변경되지 않는다")
    void original_payment_unchanged_after_transition() {
        // Given
        Payment original = createPendingPayment();
        Payment.PaymentStatus originalStatus = original.status();

        // When
        Payment completed = original.complete("TXN-123", "APPR-456");

        // Then
        assertNotSame(original, completed);
        assertEquals(originalStatus, original.status()); // 원본 상태 유지
        assertInstanceOf(Payment.PaymentStatus.Pending.class, original.status());
        assertInstanceOf(Payment.PaymentStatus.Completed.class, completed.status());
    }

    private Payment createPendingPayment() {
        return Payment.create(
                BookingId.generate(),
                MemberId.generate(),
                Money.krw(100000),
                new PaymentMethod.CreditCard(
                        "1234-****-****-5678",
                        PaymentMethod.CreditCard.CardCompany.SHINHAN,
                        0
                ),
                IdempotencyKey.generate()
        );
    }
}
