package com.ecommerce.domain.product;

import com.ecommerce.shared.Result;

/**
 * 재고용 상품 모델 - Inventory Bounded Context (Chapter 10)
 *
 * <h2>이 Context의 관심사</h2>
 * 재고 수량, 창고 위치, 안전 재고 수준 (물류 관리)
 *
 * <h2>핵심 개념: Context별 다른 연산</h2>
 * <ul>
 *   <li>reduceStock(): 주문 시 재고 차감</li>
 *   <li>addStock(): 입고 시 재고 추가</li>
 *   <li>relocate(): 창고 이동</li>
 * </ul>
 *
 * DisplayProduct에는 이런 연산이 없고, SettlementProduct에도 없다.
 * 각 Context가 자신의 책임에 맞는 연산만 제공한다.
 *
 * @see DisplayProduct 전시용 상품 (가격, 이미지)
 * @see SettlementProduct 정산용 상품 (공급가, 수수료)
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
