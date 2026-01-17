package com.travel.domain.settlement;

import com.travel.shared.types.Money;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 정산 - 판매자/파트너에게 지급할 금액 관리
 *
 * <h2>목적 (Purpose)</h2>
 * 완료된 예약에 대해 파트너(호텔, 항공사 등)에게 정산할 금액 관리
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 결합법칙</h2>
 * <pre>
 * [Key Point] 정산 금액 합산은 결합법칙을 만족:
 *   (정산1 + 정산2) + 정산3 = 정산1 + (정산2 + 정산3)
 *
 * → 병렬 처리 가능 (분산 환경에서 유용)
 * → 순서에 관계없이 동일한 결과
 * </pre>
 *
 * @param id              정산 ID
 * @param partnerId       파트너 ID
 * @param settlementDate  정산 기준일
 * @param items           정산 항목들
 * @param totalAmount     총 정산 금액
 * @param fee             수수료
 * @param netAmount       실 지급 금액 (totalAmount - fee)
 * @param status          정산 상태
 * @param createdAt       생성 시간
 */
public record Settlement(
        SettlementId id,
        String partnerId,
        LocalDate settlementDate,
        List<SettlementItem> items,
        Money totalAmount,
        Money fee,
        Money netAmount,
        SettlementStatus status,
        Instant createdAt
) {

    public Settlement {
        if (id == null) throw new IllegalArgumentException("정산 ID는 필수입니다");
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("파트너 ID는 필수입니다");
        }
        if (settlementDate == null) {
            throw new IllegalArgumentException("정산 기준일은 필수입니다");
        }
        if (items == null) items = List.of();
        else items = List.copyOf(items);
        if (totalAmount == null) throw new IllegalArgumentException("총 금액은 필수입니다");
        if (fee == null) throw new IllegalArgumentException("수수료는 필수입니다");
        if (netAmount == null) throw new IllegalArgumentException("실 지급 금액은 필수입니다");
        if (status == null) throw new IllegalArgumentException("상태는 필수입니다");
        if (createdAt == null) createdAt = Instant.now();

        // 금액 일관성 검증
        Money expected = totalAmount.subtract(fee);
        if (!expected.equals(netAmount)) {
            throw new IllegalArgumentException(
                    "금액 불일치: " + totalAmount + " - " + fee + " != " + netAmount);
        }
    }

    // ============================================
    // ID 타입
    // ============================================

    public record SettlementId(UUID value) {
        public SettlementId {
            if (value == null) throw new IllegalArgumentException("SettlementId는 null일 수 없습니다");
        }

        public static SettlementId generate() {
            return new SettlementId(UUID.randomUUID());
        }

        public static SettlementId from(String value) {
            return new SettlementId(UUID.fromString(value));
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    // ============================================
    // 정산 항목
    // ============================================

    /**
     * 정산 항목
     *
     * @param bookingId   예약 ID
     * @param productName 상품명
     * @param amount      정산 금액
     * @param completedAt 이용 완료 시간
     */
    public record SettlementItem(
            String bookingId,
            String productName,
            Money amount,
            Instant completedAt
    ) {
        public SettlementItem {
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalArgumentException("예약 ID는 필수입니다");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("상품명은 필수입니다");
            }
            if (amount == null) throw new IllegalArgumentException("금액은 필수입니다");
            if (completedAt == null) throw new IllegalArgumentException("완료 시간은 필수입니다");
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 새 정산 생성
     *
     * @param partnerId      파트너 ID
     * @param settlementDate 정산 기준일
     * @param items          정산 항목들
     * @param feeRate        수수료율 (예: 10 = 10%)
     */
    public static Settlement create(
            String partnerId,
            LocalDate settlementDate,
            List<SettlementItem> items,
            int feeRate
    ) {
        // [FC] 순수 계산
        Money totalAmount = SettlementCalculations.calculateTotalAmount(items);
        Money fee = SettlementCalculations.calculateFee(totalAmount, feeRate);
        Money netAmount = totalAmount.subtract(fee);

        return new Settlement(
                SettlementId.generate(),
                partnerId,
                settlementDate,
                items,
                totalAmount,
                fee,
                netAmount,
                new SettlementStatus.Pending(Instant.now()),
                Instant.now()
        );
    }

    // ============================================
    // Wither 패턴
    // ============================================

    public Settlement withStatus(SettlementStatus newStatus) {
        return new Settlement(
                id, partnerId, settlementDate, items,
                totalAmount, fee, netAmount, newStatus, createdAt
        );
    }

    public Settlement approve() {
        if (!(status instanceof SettlementStatus.Pending)) {
            throw new IllegalStateException("Pending 상태에서만 승인할 수 있습니다");
        }
        return withStatus(new SettlementStatus.Approved(Instant.now()));
    }

    public Settlement pay(String transactionId) {
        if (!(status instanceof SettlementStatus.Approved)) {
            throw new IllegalStateException("Approved 상태에서만 지급할 수 있습니다");
        }
        return withStatus(new SettlementStatus.Paid(Instant.now(), transactionId));
    }

    public Settlement reject(String reason) {
        if (!(status instanceof SettlementStatus.Pending)) {
            throw new IllegalStateException("Pending 상태에서만 반려할 수 있습니다");
        }
        return withStatus(new SettlementStatus.Rejected(Instant.now(), reason));
    }

    // ============================================
    // 조회 메서드
    // ============================================

    public int itemCount() {
        return items.size();
    }

    public boolean isPending() {
        return status instanceof SettlementStatus.Pending;
    }

    public boolean isPaid() {
        return status instanceof SettlementStatus.Paid;
    }
}
