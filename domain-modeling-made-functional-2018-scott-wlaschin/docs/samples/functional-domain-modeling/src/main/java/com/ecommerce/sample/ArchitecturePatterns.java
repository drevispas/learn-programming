package com.ecommerce.sample;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// =====================================================
// Chapter 8: 도메인과 외부 세계 분리
// Chapter 9: 함수형 아키텍처 패턴
// =====================================================

// ===========================================
// Ch 8.1 Persistence Ignorance
// Repository Interface (도메인 레이어에 정의)
// ===========================================

interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    void delete(OrderId id);
}

record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    OrderStatus status
) {
    public Order cancel(CancelReason reason) {
        return switch (status) {
            case OrderStatus.Unpaid u -> new Order(
                id, customerId, lines, totalAmount,
                new OrderStatus.Cancelled(LocalDateTime.now(), reason)
            );
            case OrderStatus.Paid p -> new Order(
                id, customerId, lines, totalAmount,
                new OrderStatus.Cancelled(LocalDateTime.now(), reason)
            );
            default -> throw new IllegalStateException("취소 불가능한 상태: " + status);
        };
    }
}


// ===========================================
// Ch 8.2 Trust Boundary와 DTO
// ===========================================

class OrderDto {
    public String orderId;
    public String customerId;
    public BigDecimal amount;
    public String currencyCode;
    public String status;
}

record OrderSummary(OrderId orderId, CustomerId customerId, Money amount) {}

class OrderMapper {
    private OrderMapper() {}

    public static Result<OrderSummary, String> toDomain(OrderDto dto) {
        try {
            return Result.success(new OrderSummary(
                new OrderId(dto.orderId),
                new CustomerId(Long.parseLong(dto.customerId)),
                new Money(dto.amount, Currency.valueOf(dto.currencyCode))
            ));
        } catch (IllegalArgumentException e) {
            return Result.failure("변환 실패: " + e.getMessage());
        }
    }

    public static OrderDto toDto(OrderSummary summary) {
        OrderDto dto = new OrderDto();
        dto.orderId = summary.orderId().value();
        dto.customerId = String.valueOf(summary.customerId().value());
        dto.amount = summary.amount().amount();
        dto.currencyCode = summary.amount().currency().name();
        return dto;
    }
}


// ===========================================
// Ch 8.3 Anti-Corruption Layer (ACL)
// 외부 시스템의 형식을 도메인 형식으로 변환
// ===========================================

record ExternalPaymentResponse(
    String result_code,      // "0000" = 성공
    String result_msg,
    String transaction_id
) {}

record PaymentApproval(String transactionId) {}

class PaymentGatewayAdapter {
    private final ExternalPaymentClient externalClient;

    public PaymentGatewayAdapter(ExternalPaymentClient externalClient) {
        this.externalClient = externalClient;
    }

    public Result<PaymentApproval, PaymentError> processPayment(Money amount, PaymentMethod method) {
        try {
            ExternalPaymentResponse response = externalClient.pay(amount, method);
            return translateResponse(response);
        } catch (ExternalApiException e) {
            return Result.failure(new PaymentError.SystemError(e.getMessage()));
        }
    }

    private Result<PaymentApproval, PaymentError> translateResponse(ExternalPaymentResponse response) {
        if (!"0000".equals(response.result_code())) {
            return Result.failure(translateErrorCode(response.result_code()));
        }
        return Result.success(new PaymentApproval(response.transaction_id()));
    }

    private PaymentError translateErrorCode(String code) {
        return switch (code) {
            case "1001" -> new PaymentError.InsufficientFunds(Money.ZERO);
            case "1002" -> new PaymentError.CardExpired("Unknown");
            default -> new PaymentError.SystemError("알 수 없는 오류: " + code);
        };
    }
}

interface ExternalPaymentClient {
    ExternalPaymentResponse pay(Money amount, PaymentMethod method);
}

class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) {
        super(message);
    }
}


// ===========================================
// Ch 9.1 Onion Architecture 계층 구조 예시
// ===========================================

/*
 * 계층 구조:
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │                    Infrastructure                        │
 * │  ┌─────────────────────────────────────────────────┐   │
 * │  │              Application Services               │   │
 * │  │  ┌───────────────────────────────────────────┐ │   │
 * │  │  │              Domain Model                 │ │   │
 * │  │  │  - Entities (Order, Customer)            │ │   │
 * │  │  │  - Value Objects (Money, Email)          │ │   │
 * │  │  │  - Domain Services                        │ │   │
 * │  │  └───────────────────────────────────────────┘ │   │
 * │  │  - Use Cases (PlaceOrder, CancelOrder)        │   │
 * │  │  - Repository Interfaces                       │   │
 * │  └─────────────────────────────────────────────────┘   │
 * │  - Controllers, JPA Repositories, External APIs       │
 * │  - Mappers, Adapters                                   │
 * └─────────────────────────────────────────────────────────┘
 */


// ===========================================
// Ch 9.2 함수형 의존성 주입
// ===========================================

@FunctionalInterface
interface GetExchangeRate {
    BigDecimal get(Currency from, Currency to);
}

class PriceService {
    public Money convertPrice(Money price, Currency to, GetExchangeRate getRate) {
        BigDecimal rate = getRate.get(price.currency(), to);
        return new Money(price.amount().multiply(rate), to);
    }
}


// ===========================================
// Ch 9.4 Functional Core, Imperative Shell
// 순수 도메인 서비스와 부수효과 분리
// ===========================================

class OrderDomainService {
    public PricedOrderFull calculatePrice(ValidatedOrder order) {
        return calculatePrice(order, Money.ZERO);
    }

    public PricedOrderFull calculatePrice(ValidatedOrder order, CouponType coupon) {
        Money subtotal = order.lines().stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);

        Money discount = coupon.calculateDiscount(subtotal);
        Money total = subtotal.subtract(discount);

        return new PricedOrderFull(
            order.customerId(),
            order.lines(),
            subtotal,
            discount,
            total.isNegativeOrZero() ? Money.ZERO : total
        );
    }

    private PricedOrderFull calculatePrice(ValidatedOrder order, Money discount) {
        Money subtotal = order.lines().stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);

        Money total = subtotal.subtract(discount);

        return new PricedOrderFull(
            order.customerId(),
            order.lines(),
            subtotal,
            discount,
            total.isNegativeOrZero() ? Money.ZERO : total
        );
    }

    public boolean canCancel(OrderStatus status) {
        return switch (status) {
            case OrderStatus.Unpaid u -> true;
            case OrderStatus.Paid p -> true;
            case OrderStatus.Shipping s -> false;
            case OrderStatus.Delivered d -> false;
            case OrderStatus.Cancelled c -> false;
        };
    }
}

record PricedOrderFull(
    CustomerId customerId,
    List<OrderLine> lines,
    Money subtotal,
    Money discount,
    Money totalAmount
) {}


// ===========================================
// Application Use Case (Imperative Shell)
// 부수효과를 가진 애플리케이션 서비스
// ===========================================

class PlaceOrderUseCase {
    private final OrderRepository orderRepository;
    private final PaymentGatewayAdapter paymentGateway;
    private final OrderDomainService domainService;
    private final CouponRepository couponRepository;

    public PlaceOrderUseCase(
        OrderRepository orderRepository,
        PaymentGatewayAdapter paymentGateway,
        OrderDomainService domainService,
        CouponRepository couponRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.domainService = domainService;
        this.couponRepository = couponRepository;
    }

    public Result<OrderPlaced, OrderError> execute(ValidatedOrder validated, PaymentMethod paymentMethod) {
        return execute(validated, paymentMethod, null);
    }

    public Result<OrderPlaced, OrderError> execute(
        ValidatedOrder validated,
        PaymentMethod paymentMethod,
        String couponCode
    ) {
        // 1. 쿠폰 조회 (부수효과)
        CouponType coupon = (couponCode != null)
            ? couponRepository.findByCode(couponCode).orElse(null)
            : null;

        // 2. 순수 로직: 가격 계산 (테스트 쉬움)
        PricedOrderFull priced = (coupon != null)
            ? domainService.calculatePrice(validated, coupon)
            : domainService.calculatePrice(validated);

        // 3. 결제 (부수효과)
        Result<PaymentApproval, PaymentError> paymentResult =
            paymentGateway.processPayment(priced.totalAmount(), paymentMethod);

        if (paymentResult.isFailure()) {
            String errorMsg = switch (paymentResult.error()) {
                case PaymentError.InsufficientFunds e -> "잔액 부족";
                case PaymentError.CardExpired e -> "카드 만료";
                case PaymentError.SystemError e -> e.message();
            };
            return Result.failure(new OrderError.PaymentFailed(errorMsg));
        }

        // 4. 주문 생성 및 저장 (부수효과)
        Order order = new Order(
            new OrderId("ORD-" + System.currentTimeMillis()),
            validated.customerId(),
            validated.lines(),
            priced.totalAmount(),
            new OrderStatus.Paid(
                LocalDateTime.now(),
                paymentMethod,
                paymentResult.value().transactionId()
            )
        );

        Order savedOrder = orderRepository.save(order);

        return Result.success(new OrderPlaced(
            savedOrder.id(),
            savedOrder.totalAmount(),
            LocalDateTime.now()
        ));
    }
}

interface CouponRepository {
    Optional<CouponType> findByCode(String code);
}


// ===========================================
// Currying과 부분 적용 예시
// ===========================================

class CurrencyConverter {
    private final GetExchangeRate exchangeRateProvider;
    private final PriceService priceService;

    public CurrencyConverter(GetExchangeRate exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.priceService = new PriceService();
    }

    public Function<Money, Money> toKrw() {
        return price -> priceService.convertPrice(price, Currency.KRW, exchangeRateProvider);
    }

    public Function<Money, Money> toUsd() {
        return price -> priceService.convertPrice(price, Currency.USD, exchangeRateProvider);
    }

    public Function<Money, Money> to(Currency target) {
        return price -> priceService.convertPrice(price, target, exchangeRateProvider);
    }
}
