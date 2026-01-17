package com.travel.domain.promotion;

import com.travel.domain.booking.Booking;
import com.travel.domain.booking.BookingItem;
import com.travel.domain.member.Member;
import com.travel.shared.types.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 프로모션 규칙 엔진 - Interpreter (순수 함수)
 *
 * <h2>목적 (Purpose)</h2>
 * PromotionRule AST를 해석하여 규칙 충족 여부를 평가
 *
 * <h2>핵심 개념 (Key Concept): Ch 8 Interpreter Pattern</h2>
 * <pre>
 * [Key Point] 규칙(AST)을 해석하는 인터프리터:
 *
 * PromotionRule rule = And(
 *     MembershipIs(VIP),
 *     MinimumAmount(100000)
 * );
 *
 * boolean eligible = PromotionRuleEngine.evaluate(rule, context);
 *
 * evaluate()는 rule의 타입에 따라 재귀적으로 평가:
 * - And: left와 right 모두 true일 때 true
 * - Or: left 또는 right가 true일 때 true
 * - Not: 내부 규칙이 false일 때 true
 * - Leaf 노드: context를 확인하여 조건 평가
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core</h2>
 * <pre>
 * [FC] PromotionRuleEngine.evaluate()는 순수 함수:
 * - 입력: PromotionRule, PromotionContext
 * - 출력: boolean (또는 EvaluationResult)
 * - 부수효과: 없음
 * - 동일 입력 → 동일 출력
 *
 * → Mock 없이 테스트 가능!
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 패턴 매칭</h2>
 * <pre>
 * return switch (rule) {
 *     case And(var left, var right) -> evaluate(left, ctx) && evaluate(right, ctx);
 *     case Or(var left, var right) -> evaluate(left, ctx) || evaluate(right, ctx);
 *     case Not(var inner) -> !evaluate(inner, ctx);
 *     case MembershipIs(var tier) -> ctx.member().tier() == tier;
 *     ...
 * };
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] switch에 default 사용 → 새 규칙 추가 시 누락 가능</li>
 *   <li>[Trap] 평가 중 DB 조회 → Context로 미리 데이터 전달해야 함</li>
 *   <li>[Why Context] 평가에 필요한 모든 데이터를 담은 불변 객체</li>
 * </ul>
 */
public final class PromotionRuleEngine {

    // 인스턴스화 방지
    private PromotionRuleEngine() {}

    // ============================================
    // [Key Point] 규칙 평가 컨텍스트
    // 평가에 필요한 모든 데이터를 담은 불변 객체
    // ============================================

    /**
     * 프로모션 규칙 평가 컨텍스트
     *
     * <p>[Key Point] 평가에 필요한 모든 데이터를 미리 수집하여 전달.
     * 이렇게 하면 evaluate()가 I/O 없이 순수 함수로 유지됨.</p>
     *
     * @param member       회원 정보
     * @param booking      예약 정보
     * @param evaluationDate 평가 날짜 (보통 오늘)
     * @param isFirstBooking 첫 예약 여부 (미리 조회)
     */
    public record PromotionContext(
            Member member,
            Booking booking,
            LocalDate evaluationDate,
            boolean isFirstBooking
    ) {
        public PromotionContext {
            Objects.requireNonNull(member, "member는 필수입니다");
            Objects.requireNonNull(booking, "booking은 필수입니다");
            if (evaluationDate == null) {
                evaluationDate = LocalDate.now();
            }
        }

        /**
         * 오늘 날짜로 컨텍스트 생성
         */
        public static PromotionContext of(Member member, Booking booking, boolean isFirstBooking) {
            return new PromotionContext(member, booking, LocalDate.now(), isFirstBooking);
        }
    }

    // ============================================
    // [Key Point] 평가 결과
    // 단순 boolean 대신 상세 정보 포함
    // ============================================

    /**
     * 평가 결과
     *
     * @param satisfied  규칙 충족 여부
     * @param ruleName   평가된 규칙 이름
     * @param reason     충족/불충족 사유
     */
    public record EvaluationResult(
            boolean satisfied,
            String ruleName,
            String reason
    ) {
        public static EvaluationResult success(String ruleName) {
            return new EvaluationResult(true, ruleName, "조건 충족");
        }

        public static EvaluationResult failure(String ruleName, String reason) {
            return new EvaluationResult(false, ruleName, reason);
        }
    }

    // ============================================
    // [Key Point] 메인 평가 함수 - 재귀적 인터프리터
    // ============================================

    /**
     * 프로모션 규칙 평가 (단순 boolean 반환)
     *
     * <pre>
     * [FC] 순수 함수:
     * - 입력: PromotionRule, PromotionContext
     * - 출력: boolean
     * - 부수효과: 없음
     *
     * [Ch 3] 패턴 매칭으로 각 규칙 타입별 처리
     * [Ch 8] 재귀적으로 복합 규칙 평가
     * </pre>
     *
     * @param rule    평가할 규칙
     * @param context 평가 컨텍스트
     * @return 규칙 충족 여부
     */
    public static boolean evaluate(PromotionRule rule, PromotionContext context) {
        return switch (rule) {
            // === 논리 조합 규칙 (재귀) ===
            case PromotionRule.And(var left, var right) ->
                    evaluate(left, context) && evaluate(right, context);

            case PromotionRule.Or(var left, var right) ->
                    evaluate(left, context) || evaluate(right, context);

            case PromotionRule.Not(var inner) ->
                    !evaluate(inner, context);

            // === Leaf 규칙 ===
            case PromotionRule.MembershipIs(var tier) ->
                    context.member().membershipTier() == tier;

            case PromotionRule.MinimumAmount(var minimumAmount) ->
                    context.booking().finalAmount().isGreaterThanOrEqual(minimumAmount);

            case PromotionRule.DateInRange(var start, var end) ->
                    !context.evaluationDate().isBefore(start) &&
                    !context.evaluationDate().isAfter(end);

            case PromotionRule.ItemTypeIs(var itemType) ->
                    hasItemOfType(context.booking(), itemType);

            case PromotionRule.ItemCountAtLeast(var count) ->
                    context.booking().itemCount() >= count;

            case PromotionRule.FirstBooking() ->
                    context.isFirstBooking();

            // === 항등원/영원 ===
            case PromotionRule.Always() -> true;
            case PromotionRule.Never() -> false;
        };
    }

    /**
     * 프로모션 규칙 평가 (상세 결과 반환)
     *
     * <p>디버깅이나 사용자 피드백을 위해 상세 정보 포함</p>
     *
     * @param rule    평가할 규칙
     * @param context 평가 컨텍스트
     * @return 상세 평가 결과
     */
    public static EvaluationResult evaluateWithDetails(PromotionRule rule, PromotionContext context) {
        return switch (rule) {
            // === 논리 조합 규칙 ===
            case PromotionRule.And(var left, var right) -> {
                EvaluationResult leftResult = evaluateWithDetails(left, context);
                if (!leftResult.satisfied()) {
                    yield EvaluationResult.failure("AND", "왼쪽 조건 불충족: " + leftResult.reason());
                }
                EvaluationResult rightResult = evaluateWithDetails(right, context);
                if (!rightResult.satisfied()) {
                    yield EvaluationResult.failure("AND", "오른쪽 조건 불충족: " + rightResult.reason());
                }
                yield EvaluationResult.success("AND");
            }

            case PromotionRule.Or(var left, var right) -> {
                EvaluationResult leftResult = evaluateWithDetails(left, context);
                if (leftResult.satisfied()) {
                    yield EvaluationResult.success("OR");
                }
                EvaluationResult rightResult = evaluateWithDetails(right, context);
                if (rightResult.satisfied()) {
                    yield EvaluationResult.success("OR");
                }
                yield EvaluationResult.failure("OR", "모든 조건 불충족");
            }

            case PromotionRule.Not(var inner) -> {
                boolean innerSatisfied = evaluate(inner, context);
                if (!innerSatisfied) {
                    yield EvaluationResult.success("NOT");
                }
                yield EvaluationResult.failure("NOT", "내부 조건이 충족되어 NOT 불충족");
            }

            // === Leaf 규칙 ===
            case PromotionRule.MembershipIs(var tier) -> {
                if (context.member().membershipTier() == tier) {
                    yield EvaluationResult.success("멤버십 등급");
                }
                yield EvaluationResult.failure("멤버십 등급",
                        "필요: " + tier + ", 현재: " + context.member().membershipTier());
            }

            case PromotionRule.MinimumAmount(var minimumAmount) -> {
                if (context.booking().finalAmount().isGreaterThanOrEqual(minimumAmount)) {
                    yield EvaluationResult.success("최소 금액");
                }
                yield EvaluationResult.failure("최소 금액",
                        "필요: " + minimumAmount + ", 현재: " + context.booking().finalAmount());
            }

            case PromotionRule.DateInRange dateRange -> {
                if (dateRange.contains(context.evaluationDate())) {
                    yield EvaluationResult.success("날짜 범위");
                }
                yield EvaluationResult.failure("날짜 범위",
                        "기간: " + dateRange.startDate() + "~" + dateRange.endDate() +
                        ", 현재: " + context.evaluationDate());
            }

            case PromotionRule.ItemTypeIs(var itemType) -> {
                if (hasItemOfType(context.booking(), itemType)) {
                    yield EvaluationResult.success("상품 유형");
                }
                yield EvaluationResult.failure("상품 유형", "필요한 상품 유형 없음: " + itemType);
            }

            case PromotionRule.ItemCountAtLeast(var count) -> {
                if (context.booking().itemCount() >= count) {
                    yield EvaluationResult.success("항목 수");
                }
                yield EvaluationResult.failure("항목 수",
                        "필요: " + count + "개 이상, 현재: " + context.booking().itemCount() + "개");
            }

            case PromotionRule.FirstBooking() -> {
                if (context.isFirstBooking()) {
                    yield EvaluationResult.success("첫 예약");
                }
                yield EvaluationResult.failure("첫 예약", "첫 예약이 아닙니다");
            }

            case PromotionRule.Always() -> EvaluationResult.success("항상");
            case PromotionRule.Never() -> EvaluationResult.failure("절대", "이 조건은 항상 불충족");
        };
    }

    // ============================================
    // 헬퍼 메서드
    // ============================================

    /**
     * 예약에 특정 유형의 항목이 있는지 확인
     */
    private static boolean hasItemOfType(Booking booking, PromotionRule.ItemTypeIs.ItemType itemType) {
        return booking.items().stream().anyMatch(item -> switch (itemType) {
            case ACCOMMODATION -> item instanceof BookingItem.Accommodation;
            case FLIGHT -> item instanceof BookingItem.Flight;
            case PACKAGE -> item instanceof BookingItem.TravelPackage;
        });
    }

    // ============================================
    // [Key Point] 규칙 최적화 (선택적)
    // ============================================

    /**
     * 규칙 단순화 (최적화)
     *
     * <pre>
     * [Ch 7] 대수적 속성 활용:
     * - And(Always(), rule) → rule
     * - And(Never(), rule) → Never()
     * - Or(Always(), rule) → Always()
     * - Or(Never(), rule) → rule
     * - Not(Not(rule)) → rule
     * </pre>
     *
     * @param rule 단순화할 규칙
     * @return 단순화된 규칙
     */
    public static PromotionRule simplify(PromotionRule rule) {
        return switch (rule) {
            case PromotionRule.And(var left, var right) -> {
                PromotionRule simplifiedLeft = simplify(left);
                PromotionRule simplifiedRight = simplify(right);

                // And(Always, x) = x
                if (simplifiedLeft instanceof PromotionRule.Always) {
                    yield simplifiedRight;
                }
                // And(x, Always) = x
                if (simplifiedRight instanceof PromotionRule.Always) {
                    yield simplifiedLeft;
                }
                // And(Never, x) = Never
                if (simplifiedLeft instanceof PromotionRule.Never ||
                    simplifiedRight instanceof PromotionRule.Never) {
                    yield new PromotionRule.Never();
                }
                yield new PromotionRule.And(simplifiedLeft, simplifiedRight);
            }

            case PromotionRule.Or(var left, var right) -> {
                PromotionRule simplifiedLeft = simplify(left);
                PromotionRule simplifiedRight = simplify(right);

                // Or(Always, x) = Always
                if (simplifiedLeft instanceof PromotionRule.Always ||
                    simplifiedRight instanceof PromotionRule.Always) {
                    yield new PromotionRule.Always();
                }
                // Or(Never, x) = x
                if (simplifiedLeft instanceof PromotionRule.Never) {
                    yield simplifiedRight;
                }
                // Or(x, Never) = x
                if (simplifiedRight instanceof PromotionRule.Never) {
                    yield simplifiedLeft;
                }
                yield new PromotionRule.Or(simplifiedLeft, simplifiedRight);
            }

            case PromotionRule.Not(var inner) -> {
                PromotionRule simplifiedInner = simplify(inner);
                // Not(Not(x)) = x
                if (simplifiedInner instanceof PromotionRule.Not(var doubleInner)) {
                    yield doubleInner;
                }
                // Not(Always) = Never
                if (simplifiedInner instanceof PromotionRule.Always) {
                    yield new PromotionRule.Never();
                }
                // Not(Never) = Always
                if (simplifiedInner instanceof PromotionRule.Never) {
                    yield new PromotionRule.Always();
                }
                yield new PromotionRule.Not(simplifiedInner);
            }

            // Leaf 노드는 그대로
            default -> rule;
        };
    }
}
