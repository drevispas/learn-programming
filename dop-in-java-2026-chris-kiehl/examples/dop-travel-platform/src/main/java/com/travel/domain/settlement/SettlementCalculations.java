package com.travel.domain.settlement;

import com.travel.shared.types.Currency;
import com.travel.shared.types.Money;

import java.util.List;

/**
 * 정산 계산 - 순수 함수 모음 (Functional Core)
 *
 * <h2>목적 (Purpose)</h2>
 * 정산 금액 계산을 순수 함수로 분리하여 테스트 용이성 확보
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 결합법칙 (Associativity)</h2>
 * <pre>
 * [Key Point] 정산 금액 합산은 결합법칙을 만족:
 *
 * (a + b) + c = a + (b + c)
 *
 * 예:
 * (100,000 + 200,000) + 300,000 = 600,000
 * 100,000 + (200,000 + 300,000) = 600,000
 *
 * [Why 중요]
 * 1. 순서에 관계없이 동일한 결과 → 버그 감소
 * 2. 병렬 처리 가능 → 대량 데이터 처리 시 성능 향상
 * 3. 분산 환경에서 안전한 집계 → MapReduce 패턴
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 항등원 (Identity Element)</h2>
 * <pre>
 * [Key Point] Money.ZERO는 덧셈의 항등원:
 *
 * amount + ZERO = amount
 * ZERO + amount = amount
 *
 * [사용 예]
 * items.stream()
 *     .map(SettlementItem::amount)
 *     .reduce(Money.ZERO_KRW, Money::add)  // 빈 목록이면 ZERO 반환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 분배법칙</h2>
 * <pre>
 * [Key Point] 수수료 계산에 분배법칙 적용:
 *
 * fee(a + b) = fee(a) + fee(b)  // (대략적으로, 반올림 오차 제외)
 *
 * 개별 계산 후 합산 vs 합산 후 계산 → 동일 결과
 * </pre>
 */
public final class SettlementCalculations {

    private SettlementCalculations() {}

    // ============================================
    // [Key Point] 총 금액 계산 - 결합법칙 적용
    // ============================================

    /**
     * 정산 항목들의 총 금액 계산
     *
     * <pre>
     * [FC] 순수 함수:
     * - 입력: List&lt;SettlementItem&gt;
     * - 출력: Money
     * - 부수효과: 없음
     *
     * [Ch 7] 결합법칙 + 항등원:
     * items.stream()
     *     .map(item -> item.amount())
     *     .reduce(ZERO, Money::add)  // (a + b) + c = a + (b + c)
     * </pre>
     *
     * @param items 정산 항목 목록
     * @return 총 금액
     */
    public static Money calculateTotalAmount(List<Settlement.SettlementItem> items) {
        if (items == null || items.isEmpty()) {
            return Money.ZERO_KRW;
        }

        // [Ch 7] reduce 연산 - 항등원에서 시작, 결합법칙 적용
        return items.stream()
                .map(Settlement.SettlementItem::amount)
                .reduce(
                        Money.zero(items.getFirst().amount().currency()),
                        Money::add
                );
    }

    /**
     * 여러 정산의 총 금액 합산
     *
     * <pre>
     * [Ch 7] 결합법칙 활용:
     * 병렬 처리 가능 (parallelStream 사용 가능)
     * </pre>
     *
     * @param settlements 정산 목록
     * @param currency 통화
     * @return 총 금액
     */
    public static Money calculateTotalAcrossSettlements(
            List<Settlement> settlements,
            Currency currency
    ) {
        if (settlements == null || settlements.isEmpty()) {
            return Money.zero(currency);
        }

        // [Ch 7] 병렬 처리 가능 (결합법칙 덕분에)
        return settlements.stream()
                .map(Settlement::netAmount)
                .filter(amount -> amount.currency() == currency)
                .reduce(Money.zero(currency), Money::add);
    }

    // ============================================
    // [Key Point] 수수료 계산
    // ============================================

    /**
     * 수수료 계산
     *
     * @param totalAmount 총 금액
     * @param feeRate     수수료율 (0-100)
     * @return 수수료
     */
    public static Money calculateFee(Money totalAmount, int feeRate) {
        if (feeRate <= 0) {
            return Money.zero(totalAmount.currency());
        }
        if (feeRate >= 100) {
            return totalAmount;
        }
        return totalAmount.multiplyPercent(feeRate);
    }

    /**
     * 실 지급 금액 계산
     *
     * @param totalAmount 총 금액
     * @param fee         수수료
     * @return 실 지급 금액
     */
    public static Money calculateNetAmount(Money totalAmount, Money fee) {
        return totalAmount.subtract(fee);
    }

    /**
     * 총 금액과 수수료율로 실 지급 금액 계산
     *
     * @param totalAmount 총 금액
     * @param feeRate     수수료율
     * @return 실 지급 금액
     */
    public static Money calculateNetAmount(Money totalAmount, int feeRate) {
        Money fee = calculateFee(totalAmount, feeRate);
        return calculateNetAmount(totalAmount, fee);
    }

    // ============================================
    // [Key Point] 분배 계산 - 분배법칙 적용
    // ============================================

    /**
     * 정산 금액을 비율로 분배
     *
     * <pre>
     * [Ch 7] 분배법칙 근사:
     * distribute(total, [30, 30, 40])
     * → [total*0.3, total*0.3, total*0.4]
     *
     * [주의] 반올림 오차로 합이 total과 미세하게 다를 수 있음
     * → 마지막 항목에서 조정
     * </pre>
     *
     * @param totalAmount 총 금액
     * @param percentages 분배 비율 (합 100)
     * @return 분배된 금액 목록
     */
    public static List<Money> distribute(Money totalAmount, List<Integer> percentages) {
        if (percentages == null || percentages.isEmpty()) {
            return List.of(totalAmount);
        }

        int sum = percentages.stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) {
            throw new IllegalArgumentException("비율 합이 100이 아닙니다: " + sum);
        }

        Currency currency = totalAmount.currency();
        java.util.List<Money> result = new java.util.ArrayList<>();
        Money distributed = Money.zero(currency);

        // 마지막 전까지는 비율대로 계산
        for (int i = 0; i < percentages.size() - 1; i++) {
            Money portion = totalAmount.multiplyPercent(percentages.get(i));
            result.add(portion);
            distributed = distributed.add(portion);
        }

        // 마지막은 나머지 전부 (반올림 오차 보정)
        Money remaining = totalAmount.subtract(distributed);
        result.add(remaining);

        return List.copyOf(result);
    }

    // ============================================
    // [Key Point] 통계 계산
    // ============================================

    /**
     * 기간별 총 정산 금액
     *
     * @param settlements 정산 목록
     * @param currency 통화
     * @return 총 금액
     */
    public static Money calculatePeriodTotal(
            List<Settlement> settlements,
            Currency currency
    ) {
        if (settlements == null || settlements.isEmpty()) {
            return Money.zero(currency);
        }

        return settlements.stream()
                .filter(s -> s.status() instanceof SettlementStatus.Paid)
                .map(Settlement::netAmount)
                .filter(amount -> amount.currency() == currency)
                .reduce(Money.zero(currency), Money::add);
    }

    /**
     * 파트너별 정산 금액 집계
     *
     * @param settlements 정산 목록
     * @param partnerId 파트너 ID
     * @param currency 통화
     * @return 해당 파트너의 총 정산 금액
     */
    public static Money calculatePartnerTotal(
            List<Settlement> settlements,
            String partnerId,
            Currency currency
    ) {
        if (settlements == null || settlements.isEmpty()) {
            return Money.zero(currency);
        }

        return settlements.stream()
                .filter(s -> s.partnerId().equals(partnerId))
                .map(Settlement::netAmount)
                .filter(amount -> amount.currency() == currency)
                .reduce(Money.zero(currency), Money::add);
    }
}
