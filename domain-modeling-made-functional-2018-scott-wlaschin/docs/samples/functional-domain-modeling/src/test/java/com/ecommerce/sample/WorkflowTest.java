package com.ecommerce.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowTest {
    @Test
    void workflow_returns_order_placed_on_success() {
        Workflow workflow = new Workflow();
        OrderLine line = new OrderLine(
            new ProductId("P-1"),
            new Quantity(2),
            new Money(BigDecimal.valueOf(10000), Currency.KRW)
        );
        Workflow.PlaceOrderCommand cmd = new Workflow.PlaceOrderCommand(
            "1",
            List.of(line),
            "Seoul",
            "COUPON-1"
        );

        Result<OrderPlaced, String> result = workflow.execute(cmd);

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.valueOf(20000), result.value().totalAmount().amount());
    }
}
