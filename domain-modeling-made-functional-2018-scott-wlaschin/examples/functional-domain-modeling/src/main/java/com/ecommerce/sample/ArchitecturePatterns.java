package com.ecommerce.sample;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 함수형 아키텍처 패턴 예제 (Chapter 8, 9)
 *
 * <h2>주요 패턴</h2>
 * <ul>
 *   <li><b>Persistence Ignorance</b> (Ch 8.1): Repository 인터페이스로 도메인과 저장소 분리</li>
 *   <li><b>Trust Boundary</b> (Ch 8.2): DTO와 도메인 객체 분리, 외부 데이터 검증</li>
 *   <li><b>Anti-Corruption Layer</b> (Ch 8.3): 외부 시스템 형식을 도메인 형식으로 변환</li>
 *   <li><b>Functional Core / Imperative Shell</b> (Ch 9.4): 순수 로직과 부수효과 분리</li>
 * </ul>
 *
 * <h2>핵심 개념: Functional Core / Imperative Shell (FC/IS)</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Imperative Shell (부수효과)                                  │
 * │  - Repository 호출, 외부 API 호출                           │
 * │  - 트랜잭션 관리, 로깅                                       │
 * │                                                              │
 * │  ┌─────────────────────────────────────────────────────┐   │
 * │  │ Functional Core (순수 로직)                          │   │
 * │  │  - 비즈니스 규칙, 검증, 계산                          │   │
 * │  │  - 입력 → 출력만 있음, 외부 의존성 없음               │   │
 * │  │  - 테스트 시 Mock 불필요                              │   │
 * │  └─────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <ul>
 *   <li>Functional Core: 순수 함수로 비즈니스 로직 구현 → 단위 테스트 용이</li>
 *   <li>Imperative Shell: 외부 의존성 조율 → 통합 테스트로 검증</li>
 *   <li>두 레이어를 명확히 분리하면 테스트 커버리지와 유지보수성 향상</li>
 * </ul>
 */

// =====================================================
// Chapter 8: 도메인과 외부 세계 분리
// =====================================================

// ===========================================
// Ch 8.1 Persistence Ignorance (지속성 무지)
// ===========================================

/**
 * Repository 인터페이스 - 도메인 레이어에 정의, 인프라 레이어에서 구현
 *
 * 도메인 로직이 저장소 기술(JPA, MyBatis, Redis 등)에 의존하지 않도록 한다.
 * DIP(의존성 역전 원칙)의 전형적인 적용.
 *
 * 왜 중요한가? 저장소 기술을 교체해도 도메인 로직은 변경하지 않아도 됨.
 */
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
    public Result<Order, String> cancel(CancelReason reason) {
        return switch (status) {
            case OrderStatus.Unpaid u -> Result.success(new Order(
                id, customerId, lines, totalAmount,
                new OrderStatus.Cancelled(LocalDateTime.now(), reason)
            ));
            case OrderStatus.Paid p -> Result.success(new Order(
                id, customerId, lines, totalAmount,
                new OrderStatus.Cancelled(LocalDateTime.now(), reason)
            ));
            case OrderStatus.Shipping s -> Result.failure("배송 중인 주문은 취소할 수 없습니다");
            case OrderStatus.Delivered d -> Result.failure("배송 완료된 주문은 취소할 수 없습니다");
            case OrderStatus.Cancelled c -> Result.failure("이미 취소된 주문입니다");
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
// Ch 9.4 Functional Core - 순수 도메인 서비스
// ===========================================

/**
 * 주문 도메인 서비스 - Functional Core
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>모든 메서드가 순수 함수 (입력 → 출력만, 부수효과 없음)</li>
 *   <li>외부 의존성 없음 (Repository, Gateway 등 주입받지 않음)</li>
 *   <li>동일 입력에 항상 동일 출력 → Mock 없이 단위 테스트 가능</li>
 * </ul>
 *
 * <h3>테스트 예시</h3>
 * <pre>{@code
 * // Mock 필요 없음 - 순수 함수이므로
 * var service = new OrderDomainService();
 * var priced = service.calculatePrice(validatedOrder);
 * assertEquals(expectedTotal, priced.totalAmount());
 * }</pre>
 */
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
// Ch 9.4 Imperative Shell - 유스케이스
// ===========================================

/**
 * 주문 생성 유스케이스 - Imperative Shell
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>외부 의존성(Repository, Gateway) 조율</li>
 *   <li>부수효과 실행 (DB 저장, 외부 API 호출)</li>
 *   <li>순수 로직은 {@link OrderDomainService}에 위임</li>
 * </ul>
 *
 * <h3>구조</h3>
 * <pre>
 * execute() {
 *     // 1. 부수효과: 데이터 조회
 *     var coupon = couponRepository.findByCode(code);
 *
 *     // 2. 순수 로직: 계산 (Functional Core 호출)
 *     var priced = domainService.calculatePrice(order, coupon);
 *
 *     // 3. 부수효과: 외부 API 호출
 *     var payment = paymentGateway.processPayment(priced.totalAmount());
 *
 *     // 4. 부수효과: 데이터 저장
 *     orderRepository.save(order);
 * }
 * </pre>
 *
 * <h3>테스트 전략</h3>
 * <ul>
 *   <li>단위 테스트: Functional Core (OrderDomainService)에 집중</li>
 *   <li>통합 테스트: Imperative Shell (이 클래스)에서 전체 흐름 검증</li>
 * </ul>
 */
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
