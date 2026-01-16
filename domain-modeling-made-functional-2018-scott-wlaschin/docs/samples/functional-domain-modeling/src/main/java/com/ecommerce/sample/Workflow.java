package com.ecommerce.sample;

import java.time.LocalDateTime;
import java.util.List;

public class Workflow {
    public Result<OrderPlaced, String> execute(PlaceOrderCommand command) {
        return validateOrder(command)
            .map(this::priceOrder)
            .flatMap(this::processPayment)
            .map(this::createOrderPlaced);
    }

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

    private PricedOrder priceOrder(ValidatedOrder order) {
        Money total = order.lines().stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
        return new PricedOrder(order.customerId(), total);
    }

    private Result<PricedOrder, String> processPayment(PricedOrder pricedOrder) {
        return Result.success(pricedOrder);
    }

    private OrderPlaced createOrderPlaced(PricedOrder pricedOrder) {
        return new OrderPlaced(
            new OrderId("ORD-" + System.currentTimeMillis()),
            pricedOrder.totalAmount(),
            LocalDateTime.now()
        );
    }

    public record PlaceOrderCommand(
        String customerId,
        List<OrderLine> lines,
        String shippingAddress,
        String couponCode
    ) {}
}
