package com.ecommerce.domain.product;

import com.ecommerce.shared.types.Money;

/**
 * 정산용 상품 모델 - Settlement Bounded Context (Chapter 10)
 *
 * <h2>이 Context의 관심사</h2>
 * 판매자 ID, 공급가, 수수료율 (재무 정산)
 *
 * <h2>핵심 개념: 같은 상품, 다른 관점</h2>
 * <pre>
 * DisplayProduct:    "고객이 보는 상품" - 가격 99,000원, 평점 4.5
 * InventoryProduct:  "창고의 상품" - 재고 50개, A-1-1 위치
 * SettlementProduct: "정산할 상품" - 공급가 70,000원, 수수료 10%
 * </pre>
 *
 * ProductId로 연결되지만, 각 Context가 독립적으로 발전할 수 있다.
 *
 * @see DisplayProduct 전시용 상품
 * @see InventoryProduct 재고용 상품
 */
public record SettlementProduct(
    ProductId id,
    SellerId sellerId,
    Money supplyPrice,
    FeeRate feeRate
) {
    public SettlementProduct {
        if (id == null) throw new IllegalArgumentException("ProductId required");
        if (sellerId == null) throw new IllegalArgumentException("SellerId required");
        if (supplyPrice == null) throw new IllegalArgumentException("SupplyPrice required");
        if (feeRate == null) throw new IllegalArgumentException("FeeRate required");
    }

    /**
     * 판매 수량에 대한 정산 금액 계산
     * 정산금액 = (공급가 × 수량) - 수수료
     */
    public Money calculateSettlementAmount(int quantity) {
        Money total = supplyPrice.multiply(quantity);
        Money fee = total.multiply(feeRate.value());
        return total.subtract(fee);
    }

    /**
     * 판매 수량에 대한 수수료 계산
     */
    public Money calculateFee(int quantity) {
        Money total = supplyPrice.multiply(quantity);
        return total.multiply(feeRate.value());
    }

    /**
     * 판매 수량에 대한 총 공급 금액 계산
     */
    public Money calculateTotalSupplyPrice(int quantity) {
        return supplyPrice.multiply(quantity);
    }
}
