package com.travel.domain.settlement;

import java.time.Instant;

/**
 * 정산 상태 - Sum Type
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * 정산 상태별 필요한 정보:
 * - Pending: 생성 시간
 * - Approved: 승인 시간
 * - Paid: 지급 시간, 트랜잭션 ID
 * - Rejected: 반려 시간, 반려 사유
 * </pre>
 */
public sealed interface SettlementStatus permits
        SettlementStatus.Pending,
        SettlementStatus.Approved,
        SettlementStatus.Paid,
        SettlementStatus.Rejected {

    /**
     * 정산 대기 중
     */
    record Pending(Instant createdAt) implements SettlementStatus {
        public Pending {
            if (createdAt == null) createdAt = Instant.now();
        }
    }

    /**
     * 승인됨 (지급 대기)
     */
    record Approved(Instant approvedAt) implements SettlementStatus {
        public Approved {
            if (approvedAt == null) throw new IllegalArgumentException("승인 시간은 필수입니다");
        }
    }

    /**
     * 지급 완료
     */
    record Paid(
            Instant paidAt,
            String transactionId
    ) implements SettlementStatus {
        public Paid {
            if (paidAt == null) throw new IllegalArgumentException("지급 시간은 필수입니다");
            if (transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("트랜잭션 ID는 필수입니다");
            }
        }
    }

    /**
     * 반려됨
     */
    record Rejected(
            Instant rejectedAt,
            String reason
    ) implements SettlementStatus {
        public Rejected {
            if (rejectedAt == null) throw new IllegalArgumentException("반려 시간은 필수입니다");
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("반려 사유는 필수입니다");
            }
        }
    }

    // ============================================
    // 상태 확인 헬퍼
    // ============================================

    default boolean isFinal() {
        return this instanceof Paid || this instanceof Rejected;
    }

    default String displayName() {
        return switch (this) {
            case Pending p -> "대기 중";
            case Approved a -> "승인됨";
            case Paid p -> "지급 완료";
            case Rejected r -> "반려됨";
        };
    }
}
