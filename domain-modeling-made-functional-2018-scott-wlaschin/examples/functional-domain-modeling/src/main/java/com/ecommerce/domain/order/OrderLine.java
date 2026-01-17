package com.ecommerce.domain.order;

import com.ecommerce.domain.product.ProductId;
import com.ecommerce.shared.types.Money;

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
