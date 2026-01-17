package com.ecommerce.domain.product;

import com.ecommerce.shared.types.Money;

/**
 * 정산용 상품 모델 (Settlement Context)
 * 판매자 정산에 필요한 정보만 포함
 * 전시 정보나 재고 정보는 포함하지 않음 (Bounded Context 분리)
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
