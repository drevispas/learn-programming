package com.ecommerce.domain.product;

import com.ecommerce.shared.types.Money;

import java.util.List;

/**
 * 전시용 상품 모델 - Display Bounded Context (Chapter 10)
 *
 * <h2>핵심 개념: Bounded Context 분리</h2>
 * 동일한 "상품"이지만 Context에 따라 다른 모델이 필요하다:
 * <ul>
 *   <li><b>DisplayProduct</b>: 고객 화면용 (가격, 이미지, 리뷰)</li>
 *   <li>{@link InventoryProduct}: 재고 관리용 (수량, 창고 위치)</li>
 *   <li>{@link SettlementProduct}: 판매자 정산용 (공급가, 수수료)</li>
 * </ul>
 *
 * <h2>왜 분리하는가?</h2>
 * <ul>
 *   <li>관심사 분리: 각 Context가 필요한 정보만 보유</li>
 *   <li>독립 배포: 전시 시스템 변경이 정산 시스템에 영향 없음</li>
 *   <li>팀 간 경계: Display 팀은 InventoryProduct 구조를 몰라도 됨</li>
 * </ul>
 *
 * <h2>이 Context의 관심사</h2>
 * 상품명, 설명, 이미지, 가격, 할인, 평점, 리뷰 수 (고객 경험)
 */
public record DisplayProduct(
    ProductId id,
    ProductName name,
    String description,
    List<String> imageUrls,
    Money price,
    Money originalPrice,
    double averageRating,
    int reviewCount,
    boolean isOnSale
) {
    public DisplayProduct {
        if (id == null) throw new IllegalArgumentException("ProductId required");
        if (name == null) throw new IllegalArgumentException("ProductName required");
        if (imageUrls == null) imageUrls = List.of();
        if (price == null) throw new IllegalArgumentException("Price required");
        if (averageRating < 0 || averageRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        if (reviewCount < 0) {
            throw new IllegalArgumentException("ReviewCount must be >= 0");
        }
    }

    /**
     * 할인 금액 계산
     */
    public Money discountAmount() {
        if (originalPrice == null || originalPrice.isLessThan(price)) {
            return Money.ZERO;
        }
        return originalPrice.subtract(price);
    }

    /**
     * 할인율 계산 (%)
     */
    public int discountRate() {
        if (originalPrice == null || originalPrice.isNegativeOrZero()) {
            return 0;
        }
        return (int) ((1 - price.amount().doubleValue() / originalPrice.amount().doubleValue()) * 100);
    }

    /**
     * 대표 이미지 URL
     */
    public String mainImageUrl() {
        return imageUrls.isEmpty() ? null : imageUrls.get(0);
    }
}
