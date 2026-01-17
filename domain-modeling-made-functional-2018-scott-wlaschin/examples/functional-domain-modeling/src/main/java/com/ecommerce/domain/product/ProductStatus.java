package com.ecommerce.domain.product;

import java.time.LocalDateTime;

/**
 * 상품 상태 - 임시저장, 판매중, 품절, 판매중단
 * 각 상태마다 필요한 데이터가 다름 (불가능한 상태 제거)
 */
public sealed interface ProductStatus
    permits ProductStatus.Draft, ProductStatus.OnSale, ProductStatus.SoldOut, ProductStatus.Discontinued {

    /**
     * 임시저장 상태 - 아직 등록 전
     */
    record Draft(LocalDateTime createdAt) implements ProductStatus {
        public Draft {
            if (createdAt == null) {
                createdAt = LocalDateTime.now();
            }
        }

        public Draft() {
            this(LocalDateTime.now());
        }
    }

    /**
     * 판매중 상태
     */
    record OnSale(LocalDateTime listedAt) implements ProductStatus {
        public OnSale {
            if (listedAt == null) {
                throw new IllegalArgumentException("ListedAt required");
            }
        }

        public static OnSale now() {
            return new OnSale(LocalDateTime.now());
        }
    }

    /**
     * 품절 상태 - 재입고 대기
     */
    record SoldOut(LocalDateTime soldOutAt, LocalDateTime expectedRestockAt) implements ProductStatus {
        public SoldOut {
            if (soldOutAt == null) {
                throw new IllegalArgumentException("SoldOutAt required");
            }
        }

        public SoldOut(LocalDateTime soldOutAt) {
            this(soldOutAt, null);
        }
    }

    /**
     * 판매중단 상태 - 더 이상 판매 안 함
     */
    record Discontinued(LocalDateTime discontinuedAt, String reason) implements ProductStatus {
        public Discontinued {
            if (discontinuedAt == null) {
                throw new IllegalArgumentException("DiscontinuedAt required");
            }
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reason required");
            }
        }
    }
}
