package com.travel.sample;

import com.travel.domain.booking.Booking;
import com.travel.domain.booking.BookingItem;
import com.travel.domain.member.Member;
import com.travel.domain.member.MemberId;
import com.travel.domain.membership.MembershipTier;
import com.travel.domain.promotion.PromotionRule;
import com.travel.domain.promotion.PromotionRuleEngine;
import com.travel.domain.promotion.PromotionRuleEngine.PromotionContext;
import com.travel.shared.types.DateRange;
import com.travel.shared.types.Email;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chapter 8: Rule Engine (Interpreter Pattern) 학습 테스트
 *
 * <h2>학습 목표</h2>
 * <ul>
 *   <li>"규칙을 데이터로" 표현하는 패턴 이해</li>
 *   <li>Interpreter 패턴으로 규칙 평가</li>
 *   <li>규칙 조합 (And, Or, Not)</li>
 *   <li>규칙 최적화</li>
 * </ul>
 */
@DisplayName("[Ch 8] Rule Engine - Interpreter Pattern")
class Chapter8_RuleEngineTest {

    @Nested
    @DisplayName("[Ch 8.1] 규칙을 데이터로 표현")
    class RuleAsData {

        @Test
        @DisplayName("규칙을 sealed interface로 표현한다")
        void rule_is_expressed_as_sealed_interface() {
            // [Before] 코드로 하드코딩:
            // if (member.tier == "VIP" && booking.total > 100000) { ... }

            // [After] 데이터로 표현:
            PromotionRule rule = new PromotionRule.And(
                    new PromotionRule.MembershipIs(MembershipTier.GOLD),
                    new PromotionRule.MinimumAmount(Money.krw(100000))
            );

            // [Key Point] 규칙 자체가 데이터 구조
            assertInstanceOf(PromotionRule.And.class, rule);
        }

        @Test
        @DisplayName("기본 규칙들을 조합하여 복잡한 규칙을 만든다")
        void compose_basic_rules_into_complex_rules() {
            // Given: 기본 규칙들
            PromotionRule isGoldOrPlatinum = new PromotionRule.Or(
                    new PromotionRule.MembershipIs(MembershipTier.GOLD),
                    new PromotionRule.MembershipIs(MembershipTier.PLATINUM)
            );

            PromotionRule hasMinimumAmount = new PromotionRule.MinimumAmount(Money.krw(200000));

            PromotionRule notFirstBooking = new PromotionRule.Not(
                    new PromotionRule.FirstBooking()
            );

            // When: 복잡한 규칙 조합
            PromotionRule complexRule = new PromotionRule.And(
                    isGoldOrPlatinum,
                    new PromotionRule.And(hasMinimumAmount, notFirstBooking)
            );

            // Then: 트리 구조로 표현됨
            assertInstanceOf(PromotionRule.And.class, complexRule);

            // [Key Point] 규칙을 DB에 저장하거나 API로 전달 가능
        }

        @Test
        @DisplayName("DSL 스타일로 규칙을 읽기 쉽게 작성한다")
        void dsl_style_rule_building() {
            // DSL 스타일 사용
            PromotionRule rule = PromotionRule.membershipIs(MembershipTier.GOLD)
                    .and(PromotionRule.minimumAmount(Money.krw(100000)))
                    .and(PromotionRule.itemCountAtLeast(2));

            // [Key Point] 메서드 체이닝으로 가독성 향상
            assertInstanceOf(PromotionRule.And.class, rule);
        }
    }

    @Nested
    @DisplayName("[Ch 8.2] Interpreter로 규칙 평가")
    class RuleEvaluation {

        @Test
        @DisplayName("단순 규칙을 평가한다")
        void evaluate_simple_rule() {
            // Given: 규칙과 컨텍스트
            PromotionRule rule = new PromotionRule.MembershipIs(MembershipTier.GOLD);
            PromotionContext context = createContext(MembershipTier.GOLD, Money.krw(100000));

            // When: 평가
            boolean result = PromotionRuleEngine.evaluate(rule, context);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("And 규칙은 모든 조건이 충족되어야 한다")
        void and_rule_requires_all_conditions() {
            // Given
            PromotionRule rule = new PromotionRule.And(
                    new PromotionRule.MembershipIs(MembershipTier.GOLD),
                    new PromotionRule.MinimumAmount(Money.krw(100000))
            );

            // When: 모든 조건 충족
            var satisfiedContext = createContext(MembershipTier.GOLD, Money.krw(150000));
            assertTrue(PromotionRuleEngine.evaluate(rule, satisfiedContext));

            // When: 하나의 조건 불충족
            var unsatisfiedContext = createContext(MembershipTier.SILVER, Money.krw(150000));
            assertFalse(PromotionRuleEngine.evaluate(rule, unsatisfiedContext));
        }

        @Test
        @DisplayName("Or 규칙은 하나만 충족되면 된다")
        void or_rule_requires_any_condition() {
            // Given
            PromotionRule rule = new PromotionRule.Or(
                    new PromotionRule.MembershipIs(MembershipTier.GOLD),
                    new PromotionRule.MembershipIs(MembershipTier.PLATINUM)
            );

            // When: GOLD
            var goldContext = createContext(MembershipTier.GOLD, Money.krw(100000));
            assertTrue(PromotionRuleEngine.evaluate(rule, goldContext));

            // When: PLATINUM
            var platinumContext = createContext(MembershipTier.PLATINUM, Money.krw(100000));
            assertTrue(PromotionRuleEngine.evaluate(rule, platinumContext));

            // When: SILVER (둘 다 아님)
            var silverContext = createContext(MembershipTier.SILVER, Money.krw(100000));
            assertFalse(PromotionRuleEngine.evaluate(rule, silverContext));
        }

        @Test
        @DisplayName("Not 규칙은 조건을 부정한다")
        void not_rule_negates_condition() {
            // Given
            PromotionRule rule = new PromotionRule.Not(
                    new PromotionRule.FirstBooking()
            );

            // When: 첫 예약이 아닌 경우
            var notFirstContext = createContext(MembershipTier.GOLD, Money.krw(100000), false);
            assertTrue(PromotionRuleEngine.evaluate(rule, notFirstContext));

            // When: 첫 예약인 경우
            var firstContext = createContext(MembershipTier.GOLD, Money.krw(100000), true);
            assertFalse(PromotionRuleEngine.evaluate(rule, firstContext));
        }

        @Test
        @DisplayName("복잡한 규칙을 재귀적으로 평가한다")
        void evaluate_complex_rules_recursively() {
            // Given: 복잡한 규칙
            // (GOLD OR PLATINUM) AND (금액 >= 200,000) AND (첫 예약 아님)
            PromotionRule rule = new PromotionRule.And(
                    new PromotionRule.Or(
                            new PromotionRule.MembershipIs(MembershipTier.GOLD),
                            new PromotionRule.MembershipIs(MembershipTier.PLATINUM)
                    ),
                    new PromotionRule.And(
                            new PromotionRule.MinimumAmount(Money.krw(200000)),
                            new PromotionRule.Not(new PromotionRule.FirstBooking())
                    )
            );

            // When: 모든 조건 충족
            var context = createContext(MembershipTier.GOLD, Money.krw(250000), false);
            assertTrue(PromotionRuleEngine.evaluate(rule, context));

            // When: 금액 부족 (75000 * 2박 = 150000 < 200000)
            var lowAmountContext = createContext(MembershipTier.GOLD, Money.krw(75000), false);
            assertFalse(PromotionRuleEngine.evaluate(rule, lowAmountContext));
        }
    }

    @Nested
    @DisplayName("[Ch 8.3] 상세 평가 결과")
    class DetailedEvaluation {

        @Test
        @DisplayName("평가 결과에 상세 정보를 포함한다")
        void evaluation_includes_details() {
            // Given
            PromotionRule rule = new PromotionRule.MembershipIs(MembershipTier.GOLD);
            var context = createContext(MembershipTier.SILVER, Money.krw(100000));

            // When: 상세 결과 조회
            var result = PromotionRuleEngine.evaluateWithDetails(rule, context);

            // Then: 실패 이유 확인 가능
            assertFalse(result.satisfied());
            assertTrue(result.reason().contains("GOLD"));
            assertTrue(result.reason().contains("SILVER"));
        }
    }

    @Nested
    @DisplayName("[Ch 8.4] 규칙 최적화")
    class RuleOptimization {

        @Test
        @DisplayName("And(Always, x) = x 로 단순화한다")
        void simplify_and_always() {
            // Given
            PromotionRule rule = new PromotionRule.And(
                    new PromotionRule.Always(),
                    new PromotionRule.MembershipIs(MembershipTier.GOLD)
            );

            // When: 단순화
            PromotionRule simplified = PromotionRuleEngine.simplify(rule);

            // Then: MembershipIs만 남음
            assertInstanceOf(PromotionRule.MembershipIs.class, simplified);
        }

        @Test
        @DisplayName("And(Never, x) = Never 로 단순화한다")
        void simplify_and_never() {
            // Given
            PromotionRule rule = new PromotionRule.And(
                    new PromotionRule.Never(),
                    new PromotionRule.MembershipIs(MembershipTier.GOLD)
            );

            // When: 단순화
            PromotionRule simplified = PromotionRuleEngine.simplify(rule);

            // Then: Never로 단순화
            assertInstanceOf(PromotionRule.Never.class, simplified);
        }

        @Test
        @DisplayName("Not(Not(x)) = x 로 단순화한다")
        void simplify_double_negation() {
            // Given
            PromotionRule original = new PromotionRule.MembershipIs(MembershipTier.GOLD);
            PromotionRule doubleNot = new PromotionRule.Not(
                    new PromotionRule.Not(original)
            );

            // When: 단순화
            PromotionRule simplified = PromotionRuleEngine.simplify(doubleNot);

            // Then: 원래 규칙으로 복원
            assertInstanceOf(PromotionRule.MembershipIs.class, simplified);
        }
    }

    // ============================================
    // 테스트 헬퍼
    // ============================================

    private PromotionContext createContext(MembershipTier tier, Money bookingAmount) {
        return createContext(tier, bookingAmount, false);
    }

    private PromotionContext createContext(MembershipTier tier, Money bookingAmount, boolean isFirstBooking) {
        var member = new Member(
                MemberId.generate(),
                Email.unverified("test@example.com"),
                "테스트",
                "010-1234-5678",
                tier,
                Money.krw(1000000),
                10,
                Instant.now(),
                Instant.now()
        );

        var item = new BookingItem.Accommodation(
                "ROOM-001", "호텔", "디럭스",
                new DateRange(LocalDate.now().plusDays(7), LocalDate.now().plusDays(9)),
                bookingAmount,
                2
        );

        var booking = Booking.create(member.id(), List.of(item));

        return new PromotionContext(member, booking, LocalDate.now(), isFirstBooking);
    }
}
