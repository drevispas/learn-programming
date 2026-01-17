package com.travel.domain.promotion;

import com.travel.domain.booking.Booking;
import com.travel.domain.booking.BookingItem;
import com.travel.domain.member.Member;
import com.travel.domain.membership.MembershipTier;
import com.travel.shared.types.Money;

import java.time.LocalDate;

/**
 * 프로모션 규칙 - Rule as Data (Interpreter Pattern)
 *
 * <h2>목적 (Purpose)</h2>
 * 프로모션 규칙을 데이터로 표현하여 런타임에 유연하게 조합/평가 가능하게 함
 *
 * <h2>핵심 개념 (Key Concept): Ch 8 Rule Engine</h2>
 * <pre>
 * [Key Point] "규칙"을 코드가 아닌 데이터로 표현:
 *
 * [Before] 코드로 규칙 하드코딩:
 *   if (member.grade == "VIP" && booking.total > 100000 && isWeekend()) {
 *       applyDiscount(20);
 *   }
 *   → 규칙 변경 시 코드 수정/배포 필요
 *
 * [After] 규칙을 데이터 구조로 표현:
 *   And(
 *       MembershipIs(VIP),
 *       And(
 *           MinimumAmount(100000),
 *           DateInRange(weekendDates)
 *       )
 *   )
 *   → 규칙을 DB에서 로드하거나 API로 전달 가능
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 8 Interpreter Pattern</h2>
 * <pre>
 * [Key Point] sealed interface로 규칙의 Abstract Syntax Tree(AST) 정의:
 *
 * PromotionRule (sealed interface)
 *   ├── And(left, right)         // 논리 AND
 *   ├── Or(left, right)          // 논리 OR
 *   ├── Not(rule)                // 논리 NOT
 *   ├── MembershipIs(tier)       // 멤버십 체크
 *   ├── MinimumAmount(amount)    // 최소 금액 체크
 *   ├── DateInRange(start, end)  // 날짜 범위 체크
 *   └── ItemTypeIs(type)         // 상품 유형 체크
 *
 * PromotionRuleEngine.evaluate(rule, context)로 AST 해석
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type 재귀</h2>
 * <pre>
 * [Key Point] And, Or, Not은 다른 PromotionRule을 포함:
 *   And(rule1, rule2) - rule1, rule2도 PromotionRule
 *
 * 이를 통해 복잡한 규칙을 조합:
 *   And(
 *       Or(MembershipIs(VIP), MembershipIs(GOLD)),
 *       Not(ItemTypeIs(FLIGHT))
 *   )
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] 규칙 평가 로직을 각 record에 넣음 → 인터프리터에서 분리해야 함</li>
 *   <li>[Trap] 무한 재귀 규칙 생성 → 깊이 제한 필요</li>
 *   <li>[Why sealed] 가능한 규칙 종류를 명시적으로 제한</li>
 * </ul>
 */
public sealed interface PromotionRule permits
        PromotionRule.And,
        PromotionRule.Or,
        PromotionRule.Not,
        PromotionRule.MembershipIs,
        PromotionRule.MinimumAmount,
        PromotionRule.DateInRange,
        PromotionRule.ItemTypeIs,
        PromotionRule.ItemCountAtLeast,
        PromotionRule.FirstBooking,
        PromotionRule.Always,
        PromotionRule.Never {

    // ============================================
    // [Key Point] 논리 조합 규칙
    // 다른 규칙들을 조합하여 복잡한 조건 표현
    // ============================================

    /**
     * 논리 AND - 두 규칙이 모두 충족되어야 함
     *
     * <pre>
     * [Ch 8] 사용 예:
     * And(
     *     MembershipIs(VIP),
     *     MinimumAmount(100000)
     * )
     * → VIP 멤버이고 10만원 이상 예약 시 충족
     * </pre>
     *
     * @param left  왼쪽 규칙
     * @param right 오른쪽 규칙
     */
    record And(PromotionRule left, PromotionRule right) implements PromotionRule {
        public And {
            if (left == null) throw new IllegalArgumentException("left 규칙은 필수입니다");
            if (right == null) throw new IllegalArgumentException("right 규칙은 필수입니다");
        }
    }

    /**
     * 논리 OR - 두 규칙 중 하나만 충족되면 됨
     *
     * @param left  왼쪽 규칙
     * @param right 오른쪽 규칙
     */
    record Or(PromotionRule left, PromotionRule right) implements PromotionRule {
        public Or {
            if (left == null) throw new IllegalArgumentException("left 규칙은 필수입니다");
            if (right == null) throw new IllegalArgumentException("right 규칙은 필수입니다");
        }
    }

    /**
     * 논리 NOT - 규칙이 충족되지 않아야 함
     *
     * @param rule 부정할 규칙
     */
    record Not(PromotionRule rule) implements PromotionRule {
        public Not {
            if (rule == null) throw new IllegalArgumentException("rule은 필수입니다");
        }
    }

    // ============================================
    // [Key Point] 기본 조건 규칙 (Leaf 노드)
    // ============================================

    /**
     * 멤버십 등급 조건
     *
     * @param tier 필요한 멤버십 등급
     */
    record MembershipIs(MembershipTier tier) implements PromotionRule {
        public MembershipIs {
            if (tier == null) throw new IllegalArgumentException("멤버십 등급은 필수입니다");
        }
    }

    /**
     * 최소 금액 조건
     *
     * @param minimumAmount 최소 예약 금액
     */
    record MinimumAmount(Money minimumAmount) implements PromotionRule {
        public MinimumAmount {
            if (minimumAmount == null) throw new IllegalArgumentException("최소 금액은 필수입니다");
        }
    }

    /**
     * 날짜 범위 조건
     *
     * @param startDate 시작일 (포함)
     * @param endDate   종료일 (포함)
     */
    record DateInRange(LocalDate startDate, LocalDate endDate) implements PromotionRule {
        public DateInRange {
            if (startDate == null) throw new IllegalArgumentException("시작일은 필수입니다");
            if (endDate == null) throw new IllegalArgumentException("종료일은 필수입니다");
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다");
            }
        }

        /**
         * 특정 날짜가 범위에 포함되는지 확인
         */
        public boolean contains(LocalDate date) {
            return !date.isBefore(startDate) && !date.isAfter(endDate);
        }
    }

    /**
     * 예약 항목 유형 조건
     *
     * @param itemType 예약 항목 유형 (ACCOMMODATION, FLIGHT, PACKAGE)
     */
    record ItemTypeIs(ItemType itemType) implements PromotionRule {
        public enum ItemType {
            ACCOMMODATION, FLIGHT, PACKAGE
        }

        public ItemTypeIs {
            if (itemType == null) throw new IllegalArgumentException("항목 유형은 필수입니다");
        }
    }

    /**
     * 예약 항목 수 최소 조건
     *
     * @param count 최소 항목 수
     */
    record ItemCountAtLeast(int count) implements PromotionRule {
        public ItemCountAtLeast {
            if (count <= 0) throw new IllegalArgumentException("항목 수는 1 이상이어야 합니다");
        }
    }

    /**
     * 첫 예약 조건
     *
     * <p>회원의 첫 번째 예약인 경우 충족</p>
     */
    record FirstBooking() implements PromotionRule {}

    /**
     * 항상 충족되는 규칙
     *
     * <p>[Ch 7] 항등원 역할: And(Always(), rule) = rule</p>
     */
    record Always() implements PromotionRule {}

    /**
     * 절대 충족되지 않는 규칙
     *
     * <p>[Ch 7] 영원(Zero) 역할: And(Never(), rule) = Never()</p>
     */
    record Never() implements PromotionRule {}

    // ============================================
    // [Key Point] DSL 스타일 빌더 메서드
    // 규칙 조합을 읽기 쉽게 만듦
    // ============================================

    /**
     * 현재 규칙과 다른 규칙의 AND 조합
     *
     * <pre>
     * MembershipIs(VIP)
     *     .and(MinimumAmount(100000))
     *     .and(DateInRange(startDate, endDate))
     * </pre>
     */
    default PromotionRule and(PromotionRule other) {
        return new And(this, other);
    }

    /**
     * 현재 규칙과 다른 규칙의 OR 조합
     */
    default PromotionRule or(PromotionRule other) {
        return new Or(this, other);
    }

    /**
     * 현재 규칙의 부정
     */
    default PromotionRule not() {
        return new Not(this);
    }

    // ============================================
    // 정적 팩토리 메서드 (가독성 향상)
    // ============================================

    static PromotionRule membershipIs(MembershipTier tier) {
        return new MembershipIs(tier);
    }

    static PromotionRule minimumAmount(Money amount) {
        return new MinimumAmount(amount);
    }

    static PromotionRule dateInRange(LocalDate start, LocalDate end) {
        return new DateInRange(start, end);
    }

    static PromotionRule itemTypeIs(ItemTypeIs.ItemType type) {
        return new ItemTypeIs(type);
    }

    static PromotionRule itemCountAtLeast(int count) {
        return new ItemCountAtLeast(count);
    }

    static PromotionRule firstBooking() {
        return new FirstBooking();
    }

    static PromotionRule always() {
        return new Always();
    }

    static PromotionRule never() {
        return new Never();
    }
}
