package com.ecommerce.domain.coupon;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.domain.order.OrderId;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Currency;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("쿠폰 도메인 테스트")
class CouponDomainTest {

    @Nested
    @DisplayName("CouponType 테스트")
    class CouponTypeTests {

        @Test
        @DisplayName("정액 할인: 주문 금액보다 작은 경우")
        void fixedAmount_lessThanOrder() {
            CouponType coupon = new CouponType.FixedAmount(Money.krw(3000));
            Money orderAmount = Money.krw(10000);

            Money discount = coupon.calculateDiscount(orderAmount);

            assertEquals(BigDecimal.valueOf(3000), discount.amount());
        }

        @Test
        @DisplayName("정액 할인: 주문 금액보다 큰 경우 (캡 적용)")
        void fixedAmount_greaterThanOrder() {
            CouponType coupon = new CouponType.FixedAmount(Money.krw(15000));
            Money orderAmount = Money.krw(10000);

            Money discount = coupon.calculateDiscount(orderAmount);

            assertEquals(BigDecimal.valueOf(10000), discount.amount()); // 주문 금액만큼만
        }

        @Test
        @DisplayName("정률 할인: 최대 한도 내")
        void percentage_withinLimit() {
            CouponType coupon = new CouponType.Percentage(10, Money.krw(10000));
            Money orderAmount = Money.krw(50000);

            Money discount = coupon.calculateDiscount(orderAmount);

            assertEquals(0, BigDecimal.valueOf(5000).compareTo(discount.amount())); // 10% = 5,000원
        }

        @Test
        @DisplayName("정률 할인: 최대 한도 초과 (캡 적용)")
        void percentage_exceedsLimit() {
            CouponType coupon = new CouponType.Percentage(20, Money.krw(5000));
            Money orderAmount = Money.krw(100000);

            Money discount = coupon.calculateDiscount(orderAmount);

            assertEquals(BigDecimal.valueOf(5000), discount.amount()); // 20% = 20,000원이지만 최대 5,000원
        }

        @Test
        @DisplayName("무료배송 쿠폰")
        void freeShipping() {
            CouponType coupon = new CouponType.FreeShipping(Money.krw(3000));
            Money orderAmount = Money.krw(50000);

            Money discount = coupon.calculateDiscount(orderAmount);

            assertEquals(BigDecimal.valueOf(3000), discount.amount());
        }

        @Test
        @DisplayName("쿠폰 타입별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            CouponType coupon = new CouponType.Percentage(10, Money.krw(5000));

            String result = switch (coupon) {
                case CouponType.FixedAmount f -> "정액 " + f.discountAmount().amount() + "원";
                case CouponType.Percentage p -> p.rate() + "% 할인 (최대 " + p.maxDiscount().amount() + "원)";
                case CouponType.FreeShipping s -> "무료배송";
            };

            assertEquals("10% 할인 (최대 5000원)", result);
        }
    }

    @Nested
    @DisplayName("CouponStatus 테스트")
    class CouponStatusTests {

        @Test
        @DisplayName("발급된 쿠폰 - 유효")
        void issued_valid() {
            CouponStatus.Issued issued = new CouponStatus.Issued(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
            );

            assertTrue(issued.isValid());
            assertFalse(issued.isExpired());
        }

        @Test
        @DisplayName("발급된 쿠폰 - 만료")
        void issued_expired() {
            CouponStatus.Issued issued = new CouponStatus.Issued(
                LocalDateTime.now().minusDays(31),
                LocalDateTime.now().minusDays(1)
            );

            assertFalse(issued.isValid());
            assertTrue(issued.isExpired());
        }

        @Test
        @DisplayName("사용된 쿠폰")
        void used() {
            CouponStatus.Used used = new CouponStatus.Used(
                LocalDateTime.now(),
                new OrderId("ORD-123")
            );

            assertEquals("ORD-123", used.orderId().value());
        }
    }

    @Nested
    @DisplayName("Coupon 테스트")
    class CouponTests {

        @Test
        @DisplayName("쿠폰 발급")
        void issueCoupon() {
            Coupon coupon = Coupon.issue(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                Money.krw(30000),
                LocalDateTime.now().plusDays(30)
            );

            assertTrue(coupon.isAvailable());
            assertEquals("SUMMER2024", coupon.code());
        }

        @Test
        @DisplayName("쿠폰 사용 성공")
        void useCoupon_success() {
            Coupon coupon = Coupon.issue(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                Money.krw(30000),
                LocalDateTime.now().plusDays(30)
            );

            Result<UsedCoupon, CouponError> result = coupon.use(
                new OrderId("ORD-123"),
                Money.krw(50000)
            );

            assertTrue(result.isSuccess());
            assertEquals(BigDecimal.valueOf(5000), result.value().discountAmount().amount());
            assertTrue(result.value().coupon().status() instanceof CouponStatus.Used);
        }

        @Test
        @DisplayName("쿠폰 사용 실패 - 최소 주문 금액 미달")
        void useCoupon_minOrderNotMet() {
            Coupon coupon = Coupon.issue(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                Money.krw(30000),  // 최소 30,000원
                LocalDateTime.now().plusDays(30)
            );

            Result<UsedCoupon, CouponError> result = coupon.use(
                new OrderId("ORD-123"),
                Money.krw(20000)  // 20,000원 주문
            );

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.MinOrderNotMet);
        }

        @Test
        @DisplayName("쿠폰 사용 실패 - 이미 사용됨")
        void useCoupon_alreadyUsed() {
            Coupon coupon = Coupon.issue(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                Money.krw(30000),
                LocalDateTime.now().plusDays(30)
            );

            // 첫 번째 사용
            Result<UsedCoupon, CouponError> firstUse = coupon.use(
                new OrderId("ORD-123"),
                Money.krw(50000)
            );
            assertTrue(firstUse.isSuccess());

            // 두 번째 사용 시도 (사용된 쿠폰으로)
            Coupon usedCoupon = firstUse.value().coupon();
            Result<UsedCoupon, CouponError> secondUse = usedCoupon.use(
                new OrderId("ORD-456"),
                Money.krw(50000)
            );

            assertTrue(secondUse.isFailure());
            assertTrue(secondUse.error() instanceof CouponError.AlreadyUsed);
        }

        @Test
        @DisplayName("쿠폰 사용 실패 - 만료됨")
        void useCoupon_expired() {
            Coupon coupon = new Coupon(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                new CouponStatus.Issued(
                    LocalDateTime.now().minusDays(31),
                    LocalDateTime.now().minusDays(1)  // 이미 만료
                ),
                Money.krw(30000)
            );

            Result<UsedCoupon, CouponError> result = coupon.use(
                new OrderId("ORD-123"),
                Money.krw(50000)
            );

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.Expired);
        }

        @Test
        @DisplayName("쿠폰 만료 처리")
        void expireCoupon() {
            Coupon coupon = Coupon.issue(
                new CouponId("CP-1"),
                "SUMMER2024",
                new CouponType.FixedAmount(Money.krw(5000)),
                Money.krw(30000),
                LocalDateTime.now().plusDays(30)
            );

            Coupon expired = coupon.expire();

            assertTrue(expired.status() instanceof CouponStatus.Expired);
            assertFalse(expired.isAvailable());
        }
    }

    @Nested
    @DisplayName("CouponError 테스트")
    class CouponErrorTests {

        @Test
        @DisplayName("에러 타입별 메시지")
        void errorMessages() {
            CouponError notFound = new CouponError.NotFound("INVALID");
            assertTrue(notFound.message().contains("INVALID"));

            CouponError minOrderNotMet = new CouponError.MinOrderNotMet(
                Money.krw(30000), Money.krw(20000)
            );
            assertTrue(minOrderNotMet.message().contains("30000"));

            CouponError alreadyUsed = new CouponError.AlreadyUsed(new CouponId("CP-1"));
            assertTrue(alreadyUsed.message().contains("CP-1"));
        }

        @Test
        @DisplayName("에러 타입별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            CouponError error = new CouponError.MinOrderNotMet(Money.krw(30000), Money.krw(20000));

            String result = switch (error) {
                case CouponError.NotFound e -> "쿠폰 없음";
                case CouponError.AlreadyUsed e -> "이미 사용됨";
                case CouponError.Expired e -> "만료됨";
                case CouponError.MinOrderNotMet e -> "최소 주문 금액 미달";
                case CouponError.NotAvailable e -> "사용 불가";
            };

            assertEquals("최소 주문 금액 미달", result);
        }
    }
}
