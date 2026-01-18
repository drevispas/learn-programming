package com.ecommerce.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.domain.coupon.*;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ApplyCouponUseCase 테스트
 *
 * 참고: BigDecimal 비교 시 scale이 다르면 equals가 false를 반환하므로
 * 금액 비교 시 compareTo를 사용하거나 assertMoneyEquals 헬퍼를 사용.
 */

@DisplayName("ApplyCouponUseCase 테스트")
class ApplyCouponUseCaseTest {

    private ApplyCouponUseCase useCase;
    private InMemoryCouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        useCase = new ApplyCouponUseCase(couponRepository);
        setupTestCoupons();
    }

    private void setupTestCoupons() {
        // 정액 쿠폰: 3000원 할인, 최소주문 20000원
        Coupon fixedCoupon = Coupon.issue(
            new CouponId("CP-FIXED"),
            "FIXED3000",
            new CouponType.FixedAmount(Money.krw(3000)),
            Money.krw(20000),
            LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(fixedCoupon);

        // 정률 쿠폰: 10% 할인, 최대 10000원, 최소주문 50000원
        Coupon percentCoupon = Coupon.issue(
            new CouponId("CP-PERCENT"),
            "PERCENT10",
            new CouponType.Percentage(10, Money.krw(10000)),
            Money.krw(50000),
            LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(percentCoupon);

        // 무료배송 쿠폰: 배송비 3000원 할인, 최소주문 10000원
        Coupon freeShippingCoupon = Coupon.issue(
            new CouponId("CP-SHIP"),
            "FREESHIP",
            new CouponType.FreeShipping(Money.krw(3000)),
            Money.krw(10000),
            LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(freeShippingCoupon);

        // 만료된 쿠폰 - 이미 지난 만료일을 갖는 Issued 상태로 직접 생성
        // Coupon.issue()는 issuedAt을 현재 시각으로 설정하므로, 만료된 쿠폰은 직접 생성
        Coupon expiredCoupon = new Coupon(
            new CouponId("CP-EXPIRED"),
            "EXPIRED",
            new CouponType.FixedAmount(Money.krw(5000)),
            new CouponStatus.Issued(
                LocalDateTime.now().minusDays(7), // 7일 전 발급
                LocalDateTime.now().minusDays(1)  // 어제 만료
            ),
            Money.krw(10000)
        );
        couponRepository.save(expiredCoupon);
    }

    @Nested
    @DisplayName("execute() - 실제 쿠폰 사용")
    class Execute {

        @Test
        @DisplayName("유효한 정액 쿠폰 적용 성공")
        void applyValidFixedCoupon_success() {
            // given
            ApplyCouponUseCase.ApplyCouponCommand command = new ApplyCouponUseCase.ApplyCouponCommand(
                "FIXED3000",
                "ORDER-1",
                Money.krw(30000)
            );

            // when
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(command);

            // then
            assertTrue(result.isSuccess());
            ApplyCouponUseCase.CouponApplied applied = result.value();
            assertEquals("FIXED3000", applied.couponCode());
            assertEquals(Money.krw(3000), applied.discountAmount());
            assertEquals(Money.krw(30000), applied.originalAmount());
            assertEquals(Money.krw(27000), applied.finalAmount());

            // verify coupon is marked as used
            Coupon usedCoupon = couponRepository.findByCode("FIXED3000").orElseThrow();
            assertTrue(usedCoupon.status() instanceof CouponStatus.Used);
        }

        @Test
        @DisplayName("유효한 정률 쿠폰 적용 성공")
        void applyValidPercentCoupon_success() {
            // given
            ApplyCouponUseCase.ApplyCouponCommand command = new ApplyCouponUseCase.ApplyCouponCommand(
                "PERCENT10",
                "ORDER-2",
                Money.krw(80000) // 10% = 8000원, 최대한도 10000원 미만
            );

            // when
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(command);

            // then
            assertTrue(result.isSuccess());
            ApplyCouponUseCase.CouponApplied applied = result.value();
            assertMoneyEquals(Money.krw(8000), applied.discountAmount()); // 10% of 80000
            assertMoneyEquals(Money.krw(72000), applied.finalAmount());
        }

        @Test
        @DisplayName("만료된 쿠폰 적용 실패")
        void applyExpiredCoupon_failure() {
            // given
            ApplyCouponUseCase.ApplyCouponCommand command = new ApplyCouponUseCase.ApplyCouponCommand(
                "EXPIRED",
                "ORDER-3",
                Money.krw(50000)
            );

            // when
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.Expired);
        }

        @Test
        @DisplayName("최소주문금액 미달 쿠폰 적용 실패")
        void applyWithInsufficientOrder_failure() {
            // given
            ApplyCouponUseCase.ApplyCouponCommand command = new ApplyCouponUseCase.ApplyCouponCommand(
                "FIXED3000",
                "ORDER-4",
                Money.krw(15000) // 최소 20000원 필요
            );

            // when
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.MinOrderNotMet);
            CouponError.MinOrderNotMet minOrderError = (CouponError.MinOrderNotMet) result.error();
            assertEquals(Money.krw(20000), minOrderError.required());
            assertEquals(Money.krw(15000), minOrderError.actual());
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 적용 실패")
        void applyNonExistentCoupon_failure() {
            // given
            ApplyCouponUseCase.ApplyCouponCommand command = new ApplyCouponUseCase.ApplyCouponCommand(
                "NON-EXISTENT",
                "ORDER-5",
                Money.krw(50000)
            );

            // when
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(command);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.NotFound);
        }

        @Test
        @DisplayName("이미 사용된 쿠폰 재사용 실패")
        void applyAlreadyUsedCoupon_failure() {
            // given: 먼저 쿠폰을 사용
            ApplyCouponUseCase.ApplyCouponCommand firstCommand = new ApplyCouponUseCase.ApplyCouponCommand(
                "FIXED3000",
                "ORDER-FIRST",
                Money.krw(30000)
            );
            useCase.execute(firstCommand);

            // when: 같은 쿠폰을 다시 사용 시도
            ApplyCouponUseCase.ApplyCouponCommand secondCommand = new ApplyCouponUseCase.ApplyCouponCommand(
                "FIXED3000",
                "ORDER-SECOND",
                Money.krw(30000)
            );
            Result<ApplyCouponUseCase.CouponApplied, CouponError> result = useCase.execute(secondCommand);

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.AlreadyUsed);
        }
    }

    @Nested
    @DisplayName("preview() - 할인 미리보기")
    class Preview {

        @Test
        @DisplayName("정액쿠폰 할인금액 계산")
        void previewFixedCoupon_calculatesDiscount() {
            // when
            Result<Money, CouponError> result = useCase.preview("FIXED3000", Money.krw(30000));

            // then
            assertTrue(result.isSuccess());
            assertEquals(Money.krw(3000), result.value());
        }

        @Test
        @DisplayName("정액쿠폰 - 주문금액보다 할인금액이 큰 경우 주문금액만큼 할인")
        void previewFixedCoupon_cappedAtOrderAmount() {
            // given: 3000원 할인 쿠폰이지만 최소주문금액(20000원) 이상이므로 이 케이스는 발생하기 어려움
            // 별도의 정액 쿠폰 생성
            Coupon bigFixedCoupon = Coupon.issue(
                new CouponId("CP-BIG-FIXED"),
                "BIGFIXED",
                new CouponType.FixedAmount(Money.krw(10000)),
                Money.krw(5000), // 최소주문금액 5000원
                LocalDateTime.now().plusDays(30)
            );
            couponRepository.save(bigFixedCoupon);

            // when: 7000원 주문에 10000원 할인 쿠폰
            Result<Money, CouponError> result = useCase.preview("BIGFIXED", Money.krw(7000));

            // then: 주문금액(7000원)만큼만 할인
            assertTrue(result.isSuccess());
            assertEquals(Money.krw(7000), result.value());
        }

        @Test
        @DisplayName("정률쿠폰 할인금액 계산 - 최대한도 미만")
        void previewPercentCoupon_belowMaxLimit() {
            // when: 80000원의 10% = 8000원 (최대한도 10000원 미만)
            Result<Money, CouponError> result = useCase.preview("PERCENT10", Money.krw(80000));

            // then
            assertTrue(result.isSuccess());
            assertMoneyEquals(Money.krw(8000), result.value());
        }

        @Test
        @DisplayName("정률쿠폰 할인금액 계산 - 최대한도 적용")
        void previewPercentCoupon_cappedAtMaxLimit() {
            // when: 150000원의 10% = 15000원 > 최대한도 10000원
            Result<Money, CouponError> result = useCase.preview("PERCENT10", Money.krw(150000));

            // then: 최대한도(10000원)만큼만 할인
            assertTrue(result.isSuccess());
            assertMoneyEquals(Money.krw(10000), result.value());
        }

        @Test
        @DisplayName("무료배송쿠폰 할인금액 계산")
        void previewFreeShippingCoupon_calculatesShippingFee() {
            // when
            Result<Money, CouponError> result = useCase.preview("FREESHIP", Money.krw(15000));

            // then: 배송비(3000원) 할인
            assertTrue(result.isSuccess());
            assertEquals(Money.krw(3000), result.value());
        }

        @Test
        @DisplayName("미리보기는 쿠폰 상태를 변경하지 않음")
        void preview_doesNotChangeCouponState() {
            // given
            Coupon couponBefore = couponRepository.findByCode("FIXED3000").orElseThrow();
            assertTrue(couponBefore.status() instanceof CouponStatus.Issued);

            // when
            useCase.preview("FIXED3000", Money.krw(30000));

            // then: 쿠폰 상태 변경 없음
            Coupon couponAfter = couponRepository.findByCode("FIXED3000").orElseThrow();
            assertTrue(couponAfter.status() instanceof CouponStatus.Issued);
        }

        @Test
        @DisplayName("미리보기 - 존재하지 않는 쿠폰")
        void previewNonExistentCoupon_failure() {
            // when
            Result<Money, CouponError> result = useCase.preview("NON-EXISTENT", Money.krw(50000));

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.NotFound);
        }

        @Test
        @DisplayName("미리보기 - 만료된 쿠폰")
        void previewExpiredCoupon_failure() {
            // when
            Result<Money, CouponError> result = useCase.preview("EXPIRED", Money.krw(50000));

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.NotAvailable);
        }

        @Test
        @DisplayName("미리보기 - 최소주문금액 미달")
        void previewWithInsufficientOrder_failure() {
            // when: 최소 20000원 필요
            Result<Money, CouponError> result = useCase.preview("FIXED3000", Money.krw(15000));

            // then
            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof CouponError.MinOrderNotMet);
        }
    }

    // === 헬퍼 메서드 ===

    /**
     * BigDecimal scale 차이를 무시하고 Money 금액을 비교
     * (정률 계산 시 scale이 달라질 수 있음)
     */
    private void assertMoneyEquals(Money expected, Money actual) {
        assertEquals(expected.currency(), actual.currency(),
            "Currency mismatch: expected " + expected.currency() + " but was " + actual.currency());
        assertEquals(0, expected.amount().compareTo(actual.amount()),
            "Amount mismatch: expected " + expected.amount() + " but was " + actual.amount());
    }

    // === Mock Repository 구현 ===

    static class InMemoryCouponRepository implements CouponRepository {
        private final Map<CouponId, Coupon> store = new HashMap<>();

        @Override
        public Optional<Coupon> findById(CouponId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Coupon> findByCode(String code) {
            return store.values().stream()
                .filter(c -> c.code().equals(code))
                .findFirst();
        }

        @Override
        public Coupon save(Coupon coupon) {
            store.put(coupon.id(), coupon);
            return coupon;
        }

        @Override
        public void delete(CouponId id) {
            store.remove(id);
        }
    }
}
