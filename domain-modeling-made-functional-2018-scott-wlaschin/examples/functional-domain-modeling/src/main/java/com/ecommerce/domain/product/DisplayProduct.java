package com.ecommerce.domain.product;

import com.ecommerce.shared.types.Money;

import java.util.List;

/**
 * 전시용 상품 모델 (Display Context)
 * 고객에게 보여주는 상품 정보만 포함
 * 재고 정보나 정산 정보는 포함하지 않음 (Bounded Context 분리)
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
