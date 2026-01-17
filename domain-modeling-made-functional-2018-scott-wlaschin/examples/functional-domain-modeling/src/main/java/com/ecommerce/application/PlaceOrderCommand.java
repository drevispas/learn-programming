package com.ecommerce.application;

import com.ecommerce.domain.payment.PaymentMethod;

import java.util.List;

/**
 * 주문 생성 Command - 외부 입력을 담는 DTO (Chapter 8)
 *
 * <h2>목적 (Purpose)</h2>
 * 외부(Controller, API)에서 들어온 원시 데이터를 담는다.
 * 도메인 객체가 아닌 DTO이므로, 타입 검증만 하고 비즈니스 검증은 유스케이스에서 수행.
 *
 * <h2>핵심 개념: Command vs Domain Object</h2>
 * <pre>
 * PlaceOrderCommand (DTO)          →   Order (Domain Entity)
 * - long customerId                →   CustomerId(long value)
 * - String productId               →   ProductId(String value)
 * - int quantity                   →   Quantity(int value)
 *
 * 원시 타입 → 검증된 도메인 타입으로 변환하는 과정이 Trust Boundary
 * </pre>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <ul>
 *   <li>Command는 기본적인 null 체크만 수행 (빠른 실패)</li>
 *   <li>비즈니스 검증(회원 존재 여부, 재고 확인)은 UseCase에서 수행</li>
 *   <li>검증 통과 후 도메인 타입(CustomerId, ProductId 등)으로 변환</li>
 * </ul>
 *
 * @see PlaceOrderUseCase Command를 받아 처리하는 UseCase
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
