package com.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository 인터페이스 - Bounded Context별 조회 (Chapter 8, 10)
 *
 * <h2>핵심 개념: Context별 다른 상품 모델</h2>
 * 같은 ProductId로 다른 모델을 조회한다:
 * <ul>
 *   <li>findDisplayProductById: 전시용 정보 (가격, 이미지)</li>
 *   <li>findInventoryProductById: 재고용 정보 (수량, 위치)</li>
 *   <li>findSettlementProductById: 정산용 정보 (공급가, 수수료)</li>
 * </ul>
 *
 * 실제 구현에서는 같은 DB 테이블에서 다른 컬럼을 조회하거나,
 * 다른 데이터 소스에서 조회할 수 있다.
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
