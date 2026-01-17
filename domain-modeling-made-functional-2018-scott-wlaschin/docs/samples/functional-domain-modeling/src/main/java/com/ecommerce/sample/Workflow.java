package com.ecommerce.sample;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 워크플로우 - Railway Oriented Programming으로 구현
 *
 * 파이프라인 흐름:
 * PlaceOrderCommand → ValidatedOrder → PricedOrder → OrderPlaced
 *
 * 각 단계에서 실패하면 전체 파이프라인이 실패하고 에러가 반환됨
 */
public class Workflow {

    /**
     * 기존 방식 - String 에러 (하위 호환성 유지)
     */
    public Result<OrderPlaced, String> execute(PlaceOrderCommand command) {
        return validateOrder(command)
            .map(this::priceOrder)
            .flatMap(this::processPayment)
            .map(this::createOrderPlaced);
    }

    /**
     * 개선된 방식 - 도메인 에러 타입 사용 + Validation 활용
     */
    public Result<OrderPlaced, List<OrderError>> executeWithValidation(PlaceOrderCommand command) {
        return validateOrderWithCombine(command)
            .map(this::priceOrder)
            .flatMap(this::processPaymentTyped)
            .map(this::createOrderPlaced);
    }

    // === 기존 검증 (단순 실패) ===
    private Result<ValidatedOrder, String> validateOrder(PlaceOrderCommand command) {
        if (command.lines().isEmpty()) {
            return Result.failure("Empty order");
        }
        try {
            ValidatedOrder validated = new ValidatedOrder(
                new CustomerId(Long.parseLong(command.customerId())),
                command.lines(),
                new ShippingAddress(command.shippingAddress()),
                new CouponCode(command.couponCode())
            );
            return Result.success(validated);
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    // === 개선된 검증 (Validation.combine4 사용 - 모든 에러 수집) ===
    private Result<ValidatedOrder, List<OrderError>> validateOrderWithCombine(PlaceOrderCommand command) {
        return Validation.combine4(
            validateCustomerId(command.customerId()),
            validateOrderLines(command.lines()),
            validateShippingAddress(command.shippingAddress()),
            validateCouponCode(command.couponCode()),
            ValidatedOrder::new
        ).fold(Result::success, Result::failure);
    }

    private Validation<CustomerId, List<OrderError>> validateCustomerId(String customerId) {
        try {
            long id = Long.parseLong(customerId);
            if (id <= 0) {
                return Validation.invalidOne(new OrderError.InvalidCustomerId(customerId));
            }
            return Validation.valid(new CustomerId(id));
        } catch (NumberFormatException e) {
            return Validation.invalidOne(new OrderError.InvalidCustomerId(customerId));
        }
    }

    private Validation<List<OrderLine>, List<OrderError>> validateOrderLines(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalidOne(new OrderError.EmptyOrder());
        }
        return Validation.valid(lines);
    }

    private Validation<ShippingAddress, List<OrderError>> validateShippingAddress(String address) {
        if (address == null || address.isBlank()) {
            return Validation.invalidOne(new OrderError.InvalidShippingAddress(address));
        }
        return Validation.valid(new ShippingAddress(address));
    }

    private Validation<CouponCode, List<OrderError>> validateCouponCode(String code) {
        if (code == null || code.isBlank()) {
            return Validation.invalidOne(
                new OrderError.InvalidCouponCode(code, "쿠폰 코드가 비어있습니다"));
        }
        return Validation.valid(new CouponCode(code));
    }

    // === 가격 계산 ===
    private PricedOrder priceOrder(ValidatedOrder order) {
        Money total = order.lines().stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
        return new PricedOrder(order.customerId(), total);
    }

    // === 결제 처리 ===
    private Result<PricedOrder, String> processPayment(PricedOrder pricedOrder) {
        // 실제 구현에서는 결제 게이트웨이 호출
        return Result.success(pricedOrder);
    }

    private Result<PricedOrder, List<OrderError>> processPaymentTyped(PricedOrder pricedOrder) {
        // 실제 구현에서는 결제 게이트웨이 호출
        return Result.success(pricedOrder);
    }

    // === 이벤트 생성 ===
    private OrderPlaced createOrderPlaced(PricedOrder pricedOrder) {
        return new OrderPlaced(
            new OrderId("ORD-" + System.currentTimeMillis()),
            pricedOrder.totalAmount(),
            LocalDateTime.now()
        );
    }

    // === Command DTO ===
    public record PlaceOrderCommand(
        String customerId,
        List<OrderLine> lines,
        String shippingAddress,
        String couponCode
    ) {}
}
