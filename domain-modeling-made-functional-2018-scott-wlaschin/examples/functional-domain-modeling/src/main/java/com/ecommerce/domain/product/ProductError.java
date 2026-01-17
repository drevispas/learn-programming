package com.ecommerce.domain.product;

/**
 * 상품 도메인 에러 - Sum Type으로 에러 모델링 (Chapter 6)
 *
 * 상품 관련 비즈니스 규칙 위반을 타입으로 표현.
 * InsufficientStock 에러는 요청 수량과 가용 수량을 포함하여 명확한 피드백 제공.
 */
public sealed interface ProductError
    permits ProductError.NotFound, ProductError.InsufficientStock, ProductError.NotOnSale,
            ProductError.AlreadyDiscontinued, ProductError.InvalidPrice {

    String message();

    record NotFound(ProductId productId) implements ProductError {
        @Override
        public String message() {
            return "상품을 찾을 수 없습니다: " + productId.value();
        }
    }

    record InsufficientStock(ProductId productId, int requested, int available) implements ProductError {
        @Override
        public String message() {
            return "재고 부족 [" + productId.value() + "]: 요청=" + requested + ", 재고=" + available;
        }
    }

    record NotOnSale(ProductId productId, ProductStatus currentStatus) implements ProductError {
        @Override
        public String message() {
            String statusName = switch (currentStatus) {
                case ProductStatus.Draft d -> "임시저장";
                case ProductStatus.SoldOut s -> "품절";
                case ProductStatus.Discontinued d -> "판매중단";
                case ProductStatus.OnSale o -> "판매중";
            };
            return "판매 중이 아닌 상품입니다 [" + productId.value() + "]: " + statusName;
        }
    }

    record AlreadyDiscontinued(ProductId productId) implements ProductError {
        @Override
        public String message() {
            return "이미 판매중단된 상품입니다: " + productId.value();
        }
    }

    record InvalidPrice(ProductId productId, String reason) implements ProductError {
        @Override
        public String message() {
            return "유효하지 않은 가격 [" + productId.value() + "]: " + reason;
        }
    }
}
