package com.ecommerce.domain.order;

import com.ecommerce.domain.product.ProductId;
import com.ecommerce.shared.types.Money;

/**
 * 주문 항목 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: Compound Value Object</h2>
 * 상품 ID, 수량, 단가를 하나의 단위로 묶어 주문 항목을 표현한다.
 *
 * <h2>파생 값</h2>
 * subtotal()은 저장하지 않고 필요 시 계산 (quantity * unitPrice).
 */
public record OrderLine(
    ProductId productId,
    String productName,
    Quantity quantity,
    Money unitPrice
) {
    public OrderLine {
        if (productId == null) throw new IllegalArgumentException("ProductId required");
        if (quantity == null) throw new IllegalArgumentException("Quantity required");
        if (unitPrice == null) throw new IllegalArgumentException("UnitPrice required");
    }

    public OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
        this(productId, null, quantity, unitPrice);
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
