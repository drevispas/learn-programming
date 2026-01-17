package com.travel.sample;

import com.travel.domain.coupon.CouponCalculations;
import com.travel.domain.payment.IdempotencyKey;
import com.travel.domain.settlement.Settlement;
import com.travel.domain.settlement.SettlementCalculations;
import com.travel.shared.types.Currency;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chapter 7: 대수적 속성 (결합법칙, 멱등성, 항등원) 학습 테스트
 *
 * <h2>학습 목표</h2>
 * <ul>
 *   <li>결합법칙 (Associativity) 이해 및 활용</li>
 *   <li>항등원 (Identity Element) 이해 및 활용</li>
 *   <li>멱등성 (Idempotency) 이해 및 구현</li>
 * </ul>
 */
@DisplayName("[Ch 7] 대수적 속성")
class Chapter7_AlgebraicPropertiesTest {

    @Nested
    @DisplayName("[Ch 7.1] 결합법칙 (Associativity)")
    class Associativity {

        @Test
        @DisplayName("Money 덧셈은 결합법칙을 만족한다")
        void money_addition_is_associative() {
            // Given
            Money a = Money.krw(10000);
            Money b = Money.krw(20000);
            Money c = Money.krw(30000);

            // When: 다른 그룹핑으로 계산
            Money leftGrouped = a.add(b).add(c);  // (a + b) + c
            Money rightGrouped = a.add(b.add(c)); // a + (b + c)

            // Then: 결과 동일!
            assertEquals(leftGrouped, rightGrouped);
            assertEquals(Money.krw(60000), leftGrouped);

            // [Key Point] 결합법칙 덕분에:
            // 1. 순서에 관계없이 계산 가능
            // 2. 병렬 처리 가능 (분산 환경)
        }

        @Test
        @DisplayName("정산 금액 합산은 결합법칙을 만족한다")
        void settlement_total_is_associative() {
            // Given: 정산 항목들
            var item1 = new Settlement.SettlementItem("B-001", "호텔 A", Money.krw(100000), Instant.now());
            var item2 = new Settlement.SettlementItem("B-002", "호텔 B", Money.krw(200000), Instant.now());
            var item3 = new Settlement.SettlementItem("B-003", "호텔 C", Money.krw(300000), Instant.now());

            // When: 다른 순서로 합산
            Money sum1 = SettlementCalculations.calculateTotalAmount(List.of(item1, item2, item3));
            Money sum2 = SettlementCalculations.calculateTotalAmount(List.of(item3, item1, item2));
            Money sum3 = SettlementCalculations.calculateTotalAmount(List.of(item2, item3, item1));

            // Then: 순서 무관하게 동일한 결과
            assertEquals(sum1, sum2);
            assertEquals(sum2, sum3);
            assertEquals(Money.krw(600000), sum1);
        }

        @Test
        @DisplayName("병렬 처리 시뮬레이션 - 결합법칙 활용")
        void parallel_processing_simulation() {
            // Given: 1000개의 항목
            var items = java.util.stream.IntStream.rangeClosed(1, 1000)
                    .mapToObj(i -> new Settlement.SettlementItem(
                            "B-" + i, "상품 " + i, Money.krw(1000), Instant.now()
                    ))
                    .toList();

            // When: 순차 처리
            Money sequentialTotal = SettlementCalculations.calculateTotalAmount(items);

            // When: 병렬 처리 (시뮬레이션)
            Money parallelTotal = items.parallelStream()
                    .map(Settlement.SettlementItem::amount)
                    .reduce(Money.ZERO_KRW, Money::add);

            // Then: 동일한 결과
            assertEquals(sequentialTotal, parallelTotal);
            assertEquals(Money.krw(1000000), sequentialTotal);

            // [Key Point] 결합법칙이 없으면 병렬 처리 결과가 달라질 수 있음!
        }
    }

    @Nested
    @DisplayName("[Ch 7.2] 항등원 (Identity Element)")
    class IdentityElement {

        @Test
        @DisplayName("Money.ZERO_KRW는 덧셈의 항등원이다")
        void money_zero_is_identity_for_addition() {
            // Given
            Money amount = Money.krw(50000);
            Money zero = Money.ZERO_KRW;

            // Then: x + 0 = x
            assertEquals(amount, amount.add(zero));
            // Then: 0 + x = x
            assertEquals(amount, zero.add(amount));

            // [Key Point] 빈 목록을 합산할 때 항등원 사용:
            // items.stream().reduce(ZERO, Money::add)
        }

        @Test
        @DisplayName("빈 목록의 합산 결과는 항등원이다")
        void empty_list_sum_is_identity() {
            // Given: 빈 목록
            List<Settlement.SettlementItem> emptyItems = List.of();

            // When
            Money total = SettlementCalculations.calculateTotalAmount(emptyItems);

            // Then: 항등원 반환
            assertEquals(Money.ZERO_KRW, total);
        }

        @Test
        @DisplayName("0% 할인 쿠폰은 항등원이다")
        void zero_discount_coupon_is_identity() {
            // Given
            Money originalPrice = Money.krw(100000);

            // When: 0원 할인 적용
            Money discountedPrice = CouponCalculations.calculateFinalAmount(
                    originalPrice,
                    Money.ZERO_KRW
            );

            // Then: 원래 가격과 동일
            assertEquals(originalPrice, discountedPrice);
        }
    }

    @Nested
    @DisplayName("[Ch 7.3] 멱등성 (Idempotency)")
    class Idempotency {

        @Test
        @DisplayName("멱등성 키는 중복 처리를 방지한다")
        void idempotency_key_prevents_duplicates() {
            // Given: 동일한 멱등성 키
            IdempotencyKey key = IdempotencyKey.from("unique-operation-123");

            // When: 동일 키로 여러 번 처리 시도
            // [실제 시스템] 두 번째 요청은 기존 결과 반환

            // Then: 키가 동일하면 동일한 작업을 식별
            IdempotencyKey key2 = IdempotencyKey.from("unique-operation-123");
            assertEquals(key.key(), key2.key());

            // [Key Point] f(f(x)) = f(x)
            // 같은 작업을 여러 번 수행해도 결과가 동일
        }

        @Test
        @DisplayName("멱등성 키는 만료된다")
        void idempotency_key_expires() throws InterruptedException {
            // Given: 매우 짧은 TTL의 키
            var key = new IdempotencyKey(
                    "test-key",
                    Instant.now(),
                    Instant.now().plusMillis(100) // 100ms 후 만료
            );

            // Then: 처음에는 유효
            assertTrue(key.isValid());

            // When: 만료 대기
            Thread.sleep(150);

            // Then: 만료됨
            assertTrue(key.isExpired());
            assertFalse(key.isValid());
        }

        @Test
        @DisplayName("멱등성이 필요한 상황 - 결제 시나리오")
        void idempotency_in_payment_scenario() {
            // 시나리오:
            // 1. 사용자가 "결제" 버튼 클릭
            // 2. 네트워크 지연으로 응답 못 받음
            // 3. 사용자가 다시 "결제" 버튼 클릭
            // 4. 멱등성 없으면 → 이중 결제!
            //    멱등성 있으면 → 기존 결제 결과 반환

            // [구현 패턴]
            IdempotencyKey key = IdempotencyKey.generate(); // 클라이언트에서 생성

            // 첫 번째 요청
            var firstResult = simulatePayment(key, 100000);

            // 두 번째 요청 (같은 키)
            var secondResult = simulatePayment(key, 100000);

            // 결과가 동일해야 함 (이중 결제 아님)
            assertEquals(firstResult, secondResult);
        }

        // 멱등성 시뮬레이션 (실제로는 DB 체크 필요)
        private java.util.Map<String, String> processedPayments = new java.util.concurrent.ConcurrentHashMap<>();

        private String simulatePayment(IdempotencyKey key, long amount) {
            // 이미 처리된 키인지 확인
            String existingResult = processedPayments.get(key.key());
            if (existingResult != null) {
                return existingResult; // 기존 결과 반환 (멱등성)
            }

            // 새 결제 처리
            String result = "PAYMENT-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            processedPayments.put(key.key(), result);
            return result;
        }
    }

    @Nested
    @DisplayName("[Ch 7.4] 분배법칙")
    class Distributivity {

        @Test
        @DisplayName("수수료 계산은 분배법칙을 근사적으로 만족한다")
        void fee_calculation_is_approximately_distributive() {
            // Given
            Money amount1 = Money.krw(100000);
            Money amount2 = Money.krw(200000);
            int feeRate = 10; // 10%

            // When: 개별 계산 후 합산
            Money fee1 = SettlementCalculations.calculateFee(amount1, feeRate);
            Money fee2 = SettlementCalculations.calculateFee(amount2, feeRate);
            Money totalFeeIndividual = fee1.add(fee2);

            // When: 합산 후 계산
            Money totalAmount = amount1.add(amount2);
            Money totalFeeCombined = SettlementCalculations.calculateFee(totalAmount, feeRate);

            // Then: 결과가 동일 (반올림 오차 범위 내)
            assertEquals(totalFeeCombined, totalFeeIndividual);
            assertEquals(Money.krw(30000), totalFeeCombined);

            // [Key Point] fee(a + b) = fee(a) + fee(b)
            // 분배법칙 덕분에 개별 계산과 일괄 계산 결과가 동일
        }
    }
}
