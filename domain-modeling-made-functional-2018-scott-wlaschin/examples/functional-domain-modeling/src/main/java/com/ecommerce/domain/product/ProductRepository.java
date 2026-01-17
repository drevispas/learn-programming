package com.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 레포지토리 인터페이스
 * 도메인 레이어에 정의되어 인프라 레이어에서 구현
 */
public interface ProductRepository {
    Optional<DisplayProduct> findDisplayProductById(ProductId id);
    Optional<InventoryProduct> findInventoryProductById(ProductId id);
    Optional<SettlementProduct> findSettlementProductById(ProductId id);

    List<DisplayProduct> findDisplayProductsByIds(List<ProductId> ids);
    List<InventoryProduct> findInventoryProductsByIds(List<ProductId> ids);

    InventoryProduct saveInventoryProduct(InventoryProduct product);

    boolean existsById(ProductId id);
}
