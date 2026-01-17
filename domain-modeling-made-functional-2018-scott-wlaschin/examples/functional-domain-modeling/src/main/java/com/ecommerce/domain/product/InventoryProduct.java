package com.ecommerce.domain.product;

import com.ecommerce.shared.Result;

/**
 * 재고용 상품 모델 (Inventory Context)
 * 창고 관리에 필요한 정보만 포함
 * 가격 정보나 전시 정보는 포함하지 않음 (Bounded Context 분리)
 */
public record InventoryProduct(
    ProductId id,
    StockQuantity stock,
    WarehouseLocation location,
    int safetyStock
) {
    public InventoryProduct {
        if (id == null) throw new IllegalArgumentException("ProductId required");
        if (stock == null) throw new IllegalArgumentException("Stock required");
        if (location == null) throw new IllegalArgumentException("Location required");
        if (safetyStock < 0) throw new IllegalArgumentException("SafetyStock must be >= 0");
    }

    /**
     * 주어진 수량만큼 재고가 있는지 확인
     */
    public boolean isAvailable(int quantity) {
        return stock.hasEnough(quantity);
    }

    /**
     * 품절 여부
     */
    public boolean isOutOfStock() {
        return stock.isOutOfStock();
    }

    /**
     * 안전 재고 이하로 떨어졌는지 확인
     */
    public boolean isBelowSafetyStock() {
        return stock.value() < safetyStock;
    }

    /**
     * 재고 차감
     * @return 성공 시 갱신된 InventoryProduct, 실패 시 ProductError
     */
    public Result<InventoryProduct, ProductError> reduceStock(int quantity) {
        if (!isAvailable(quantity)) {
            return Result.failure(new ProductError.InsufficientStock(id, quantity, stock.value()));
        }
        return Result.success(new InventoryProduct(
            id,
            stock.subtract(quantity),
            location,
            safetyStock
        ));
    }

    /**
     * 재고 추가 (입고)
     */
    public InventoryProduct addStock(int quantity) {
        return new InventoryProduct(
            id,
            stock.add(quantity),
            location,
            safetyStock
        );
    }

    /**
     * 창고 위치 변경
     */
    public InventoryProduct relocate(WarehouseLocation newLocation) {
        return new InventoryProduct(id, stock, newLocation, safetyStock);
    }
}
