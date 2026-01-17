package com.ecommerce.application;

import com.ecommerce.domain.payment.PaymentMethod;

import java.util.List;

/**
 * 주문 생성 명령 DTO
 */
public record PlaceOrderCommand(
    long customerId,
    List<OrderLineItem> items,
    String recipientName,
    String phoneNumber,
    String zipCode,
    String address,
    String detailAddress,
    PaymentMethod paymentMethod,
    String couponCode
) {
    public PlaceOrderCommand {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items required");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("PaymentMethod required");
        }
    }

    /**
     * 주문 항목 DTO
     */
    public record OrderLineItem(
        String productId,
        int quantity
    ) {
        public OrderLineItem {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("ProductId required");
            }
            if (quantity < 1) {
                throw new IllegalArgumentException("Quantity must be >= 1");
            }
        }
    }
}
