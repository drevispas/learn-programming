package com.travel.domain.booking;

import com.travel.shared.types.Currency;
import com.travel.shared.types.Money;

import java.util.List;

/**
 * 예약 계산 - 순수 함수 모음 (Functional Core)
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 관련 비즈니스 로직을 순수 함수로 분리하여 테스트 용이성 확보
 *
 * <h2>핵심 개념 (Key Concept): Ch 1 DOP 원칙 - 데이터와 동작 분리</h2>
 * <pre>
 * [Key Point] Booking record = 순수 데이터
 *             BookingCalculations = 순수 함수 (계산 로직)
 *
 * [Why 분리]
 * 1. 테스트 용이: 순수 함수는 입력 → 출력만 검증하면 됨 (Mock 불필요)
 * 2. 재사용성: 동일 계산을 다른 컨텍스트에서 재사용 가능
 * 3. 추론 용이: 부수효과 없으므로 코드 이해가 쉬움
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core</h2>
 * <pre>
 * [FC] BookingCalculations - 순수 함수만
 *   - 외부 의존성 없음
 *   - I/O 없음
 *   - 상태 변경 없음
 *   - 입력이 같으면 출력도 항상 같음
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 결합법칙</h2>
 * <pre>
 * [Key Point] 금액 계산은 결합법칙을 만족:
 *   (item1 + item2) + item3 = item1 + (item2 + item3)
 *
 * → 순서에 관계없이 동일한 결과 보장
 * → 병렬 처리 가능
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] 계산 중 DB 조회 → 순수성 위반, Imperative Shell에서 해야 함</li>
 *   <li>[Trap] 계산 결과를 객체에 저장 → 상태 변경, 대신 새 값 반환</li>
 *   <li>[Why static] 인스턴스 상태 없음을 명시</li>
 * </ul>
 */
public final class BookingCalculations {

    // 인스턴스화 방지
    private BookingCalculations() {}

    // ============================================
    // [Key Point] 금액 계산 - 순수 함수
    // ============================================

    /**
     * 예약 항목들의 총 금액 계산
     *
     * <pre>
     * [FC] 순수 함수:
     * - 입력: List&lt;BookingItem&gt;
     * - 출력: Money
     * - 부수효과: 없음
     *
     * [Ch 7] 결합법칙 적용:
     * items.stream().reduce(ZERO, Money::add)
     * → 순서에 관계없이 동일한 합계
     * </pre>
     *
     * @param items 예약 항목 목록
     * @return 총 금액
     */
    public static Money calculateTotalAmount(List<BookingItem> items) {
        if (items == null || items.isEmpty()) {
            return Money.ZERO_KRW;
        }

        // [Ch 7] reduce 연산 - 항등원(ZERO)에서 시작하여 결합
        return items.stream()
                .map(BookingItem::basePrice)
                .reduce(Money.zero(items.getFirst().basePrice().currency()), Money::add);
    }

    /**
     * 할인 적용 후 최종 금액 계산
     *
     * @param totalAmount 총 금액
     * @param discountAmount 할인 금액
     * @return 최종 결제 금액
     */
    public static Money calculateFinalAmount(Money totalAmount, Money discountAmount) {
        if (discountAmount == null || discountAmount.isZero()) {
            return totalAmount;
        }
        return totalAmount.subtract(discountAmount);
    }

    /**
     * 백분율 할인 금액 계산
     *
     * @param totalAmount 총 금액
     * @param discountPercent 할인율 (0-100)
     * @return 할인 금액
     */
    public static Money calculatePercentDiscount(Money totalAmount, int discountPercent) {
        if (discountPercent <= 0) {
            return Money.zero(totalAmount.currency());
        }
        if (discountPercent >= 100) {
            return totalAmount;
        }
        return totalAmount.multiplyPercent(discountPercent);
    }

    /**
     * 정액 할인과 백분율 할인 중 더 큰 금액 선택
     *
     * @param totalAmount     총 금액
     * @param fixedDiscount   정액 할인
     * @param percentDiscount 백분율 할인율
     * @return 더 유리한 할인 금액
     */
    public static Money calculateBestDiscount(
            Money totalAmount,
            Money fixedDiscount,
            int percentDiscount
    ) {
        Money percentAmount = calculatePercentDiscount(totalAmount, percentDiscount);
        return fixedDiscount.isGreaterThan(percentAmount) ? fixedDiscount : percentAmount;
    }

    // ============================================
    // [Key Point] 취소/환불 계산 - 순수 함수
    // ============================================

    /**
     * 환불 금액 계산
     *
     * <pre>
     * 환불 정책:
     * - 이용일 7일 전: 100% 환불
     * - 이용일 3일 전: 70% 환불
     * - 이용일 1일 전: 50% 환불
     * - 이용일 당일: 환불 불가
     * </pre>
     *
     * @param paidAmount 결제 금액
     * @param daysBeforeUse 이용일까지 남은 일수
     * @return 환불 금액
     */
    public static Money calculateRefundAmount(Money paidAmount, long daysBeforeUse) {
        int refundPercent = switch ((int) daysBeforeUse) {
            case int d when d >= 7 -> 100;
            case int d when d >= 3 -> 70;
            case int d when d >= 1 -> 50;
            default -> 0;
        };

        return paidAmount.multiplyPercent(refundPercent);
    }

    /**
     * 노쇼 패널티 금액 계산
     *
     * @param paidAmount 결제 금액
     * @param penaltyPercent 패널티 비율 (기본 30%)
     * @return 패널티 금액
     */
    public static Money calculateNoShowPenalty(Money paidAmount, int penaltyPercent) {
        return paidAmount.multiplyPercent(penaltyPercent);
    }

    /**
     * 기본 노쇼 패널티 (30%)
     */
    public static Money calculateNoShowPenalty(Money paidAmount) {
        return calculateNoShowPenalty(paidAmount, 30);
    }

    // ============================================
    // [Key Point] 검증 함수 - 순수 함수
    // ============================================

    /**
     * 최소 결제 금액 검증
     *
     * @param amount 금액
     * @param minimumAmount 최소 금액
     * @return 최소 금액 이상이면 true
     */
    public static boolean isMinimumAmountMet(Money amount, Money minimumAmount) {
        return amount.isGreaterThanOrEqual(minimumAmount);
    }

    /**
     * 최대 항목 수 검증
     *
     * @param items 항목 목록
     * @param maxItems 최대 항목 수
     * @return 최대 항목 수 이하면 true
     */
    public static boolean isWithinItemLimit(List<BookingItem> items, int maxItems) {
        return items != null && items.size() <= maxItems;
    }

    /**
     * 통화 일관성 검증
     *
     * @param items 항목 목록
     * @return 모든 항목의 통화가 동일하면 true
     */
    public static boolean hasSameCurrency(List<BookingItem> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        Currency first = items.getFirst().basePrice().currency();
        return items.stream()
                .map(item -> item.basePrice().currency())
                .allMatch(currency -> currency == first);
    }

    // ============================================
    // [Key Point] 통계 계산 - 순수 함수
    // ============================================

    /**
     * 예약 목록에서 총 매출 계산
     *
     * <pre>
     * [Ch 7] 결합법칙 + 항등원:
     * - 빈 목록이면 항등원(ZERO) 반환
     * - 순서 무관하게 합산 가능
     * </pre>
     */
    public static Money calculateTotalRevenue(List<Booking> bookings, Currency currency) {
        if (bookings == null || bookings.isEmpty()) {
            return Money.zero(currency);
        }

        return bookings.stream()
                .filter(b -> b.isConfirmed() || b.status() instanceof BookingStatus.Completed)
                .map(Booking::finalAmount)
                .filter(amount -> amount.currency() == currency)
                .reduce(Money.zero(currency), Money::add);
    }

    /**
     * 예약 목록에서 평균 예약 금액 계산
     */
    public static Money calculateAverageBookingAmount(List<Booking> bookings, Currency currency) {
        if (bookings == null || bookings.isEmpty()) {
            return Money.zero(currency);
        }

        List<Money> amounts = bookings.stream()
                .filter(b -> b.isConfirmed() || b.status() instanceof BookingStatus.Completed)
                .map(Booking::finalAmount)
                .filter(amount -> amount.currency() == currency)
                .toList();

        if (amounts.isEmpty()) {
            return Money.zero(currency);
        }

        Money total = amounts.stream().reduce(Money.zero(currency), Money::add);
        return new Money(
                total.amount().divide(java.math.BigDecimal.valueOf(amounts.size()), java.math.RoundingMode.HALF_UP),
                currency
        );
    }
}
