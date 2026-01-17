package com.ecommerce.application;

import com.ecommerce.domain.coupon.Coupon;
import com.ecommerce.domain.coupon.CouponRepository;
import com.ecommerce.domain.coupon.UsedCoupon;
import com.ecommerce.domain.member.Member;
import com.ecommerce.domain.member.MemberError;
import com.ecommerce.domain.member.MemberId;
import com.ecommerce.domain.member.MemberRepository;
import com.ecommerce.domain.order.CustomerId;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.domain.order.OrderLine;
import com.ecommerce.domain.order.OrderPlaced;
import com.ecommerce.domain.order.OrderRepository;
import com.ecommerce.domain.order.Quantity;
import com.ecommerce.domain.order.ShippingAddress;
import com.ecommerce.domain.payment.PaymentError;
import com.ecommerce.domain.payment.PaymentGateway;
import com.ecommerce.domain.product.DisplayProduct;
import com.ecommerce.domain.product.InventoryProduct;
import com.ecommerce.domain.product.ProductError;
import com.ecommerce.domain.product.ProductId;
import com.ecommerce.domain.product.ProductRepository;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 주문 생성 유스케이스 - Imperative Shell (Chapter 9)
 *
 * <h2>목적 (Purpose)</h2>
 * 외부 의존성(Repository, Gateway)을 조율하고 부수효과를 실행한다.
 * 순수 비즈니스 로직은 {@link OrderDomainService}(Functional Core)에 위임한다.
 *
 * <h2>핵심 개념 (Key Concept): Functional Core / Imperative Shell</h2>
 * <pre>
 * execute() 메서드 구조:
 * ┌────────────────────────────────────────────────────────────┐
 * │ 1. 데이터 조회 (부수효과)                                    │
 * │    memberRepository.findById(), productRepository.find...  │
 * │                                                             │
 * │ 2. 순수 로직 호출 (Functional Core)                         │
 * │    domainService.calculatePricing() - Mock 없이 테스트 가능  │
 * │                                                             │
 * │ 3. 외부 API 호출 (부수효과)                                  │
 * │    paymentGateway.charge()                                  │
 * │                                                             │
 * │ 4. 데이터 저장 (부수효과)                                    │
 * │    orderRepository.save(), couponRepository.save()         │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <ul>
 *   <li>Imperative Shell은 "껍데기" 역할 - I/O만 담당</li>
 *   <li>복잡한 비즈니스 로직은 Functional Core로 분리하여 테스트 용이성 확보</li>
 *   <li>Result 타입으로 에러 흐름을 명시적으로 관리</li>
 * </ul>
 *
 * @see OrderDomainService Functional Core - 순수 비즈니스 로직
 * @see PlaceOrderError 여러 Bounded Context 에러를 통합한 에러 타입
 */
public class PlaceOrderUseCase {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final OrderDomainService domainService;

    public PlaceOrderUseCase(
        MemberRepository memberRepository,
        ProductRepository productRepository,
        CouponRepository couponRepository,
        OrderRepository orderRepository,
        PaymentGateway paymentGateway,
        OrderDomainService domainService
    ) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.domainService = domainService;
    }

    /**
     * 주문 처리 실행 - 워크플로우의 진입점
     *
     * 각 단계가 Result를 반환하여 실패 시 조기 종료된다.
     * 부수효과와 순수 로직이 번갈아 실행되는 구조에 주목.
     */
    public Result<OrderPlaced, PlaceOrderError> execute(PlaceOrderCommand command) {
        // ========================================
        // 1. 회원 조회 [부수효과: Repository I/O]
        // ========================================
        Optional<Member> memberOpt = memberRepository.findById(new MemberId(command.customerId()));
        if (memberOpt.isEmpty()) {
            return Result.failure(new PlaceOrderError.MemberIssue(
                new MemberError.NotFound(new MemberId(command.customerId()))
            ));
        }
        Member member = memberOpt.get();

        // ========================================
        // 2. 상품 조회 및 재고 확인 [부수효과]
        // ========================================
        Result<List<OrderLineWithProduct>, PlaceOrderError> lineResult = prepareOrderLines(command.items());
        if (lineResult.isFailure()) {
            return Result.failure(lineResult.error());
        }
        List<OrderLineWithProduct> linesWithProducts = lineResult.value();
        List<OrderLine> orderLines = linesWithProducts.stream()
            .map(OrderLineWithProduct::orderLine)
            .toList();

        // ========================================
        // 3. 쿠폰 조회 및 검증 [부수효과]
        // ========================================
        UsedCoupon usedCoupon = null;
        if (command.couponCode() != null && !command.couponCode().isBlank()) {
            Result<UsedCoupon, PlaceOrderError> couponResult = applyCoupon(
                command.couponCode(),
                domainService.calculateSubtotal(orderLines)
            );
            if (couponResult.isFailure()) {
                return Result.failure(couponResult.error());
            }
            usedCoupon = couponResult.value();
        }

        // ========================================
        // 4. 가격 계산 [순수 로직: Functional Core]
        //    → Mock 없이 단위 테스트 가능
        // ========================================
        OrderDomainService.PricingResult pricing = usedCoupon != null
            ? domainService.calculatePricing(orderLines, member, usedCoupon.coupon().type())
            : domainService.calculatePricing(orderLines, member);

        // 5. 배송지 생성
        ShippingAddress shippingAddress = new ShippingAddress(
            command.recipientName() != null ? command.recipientName() : member.name(),
            command.phoneNumber(),
            command.zipCode(),
            command.address(),
            command.detailAddress()
        );

        // ========================================
        // 6. 결제 처리 [부수효과: 외부 API]
        // ========================================
        Result<String, PaymentError> paymentResult = paymentGateway.charge(
            pricing.totalAmount(),
            command.paymentMethod()
        );
        if (paymentResult.isFailure()) {
            return Result.failure(new PlaceOrderError.PaymentIssue(paymentResult.error()));
        }
        String transactionId = paymentResult.value();

        // ========================================
        // 7. 주문 생성 [순수 로직]
        // ========================================
        Order order = domainService.createOrder(
            new CustomerId(command.customerId()),
            orderLines,
            shippingAddress,
            pricing.totalDiscount(),
            pricing.shippingFee()
        );

        // 8. 결제 완료 처리
        Result<Order, com.ecommerce.domain.order.OrderError> paidResult = order.pay(
            command.paymentMethod(),
            transactionId
        );
        if (paidResult.isFailure()) {
            return Result.failure(new PlaceOrderError.OrderIssue(paidResult.error()));
        }
        Order paidOrder = paidResult.value();

        // 9. 재고 차감
        for (OrderLineWithProduct lwp : linesWithProducts) {
            Result<InventoryProduct, ProductError> reduceResult = lwp.inventory()
                .reduceStock(lwp.orderLine().quantity().value());
            if (reduceResult.isFailure()) {
                // 롤백: 실제 환경에서는 트랜잭션 처리 필요
                return Result.failure(new PlaceOrderError.ProductIssue(reduceResult.error()));
            }
            productRepository.saveInventoryProduct(reduceResult.value());
        }

        // 10. 쿠폰 사용 처리
        if (usedCoupon != null) {
            couponRepository.save(usedCoupon.coupon());
        }

        // 11. 주문 저장
        Order savedOrder = orderRepository.save(paidOrder);

        // 12. 이벤트 생성
        return Result.success(OrderPlaced.from(savedOrder));
    }

    /**
     * 주문 항목 준비 (상품 조회 + 재고 확인)
     */
    private Result<List<OrderLineWithProduct>, PlaceOrderError> prepareOrderLines(
        List<PlaceOrderCommand.OrderLineItem> items
    ) {
        List<OrderLineWithProduct> result = new ArrayList<>();

        for (PlaceOrderCommand.OrderLineItem item : items) {
            ProductId productId = new ProductId(item.productId());

            // 전시 상품 조회 (가격 정보)
            Optional<DisplayProduct> displayOpt = productRepository.findDisplayProductById(productId);
            if (displayOpt.isEmpty()) {
                return Result.failure(new PlaceOrderError.ProductIssue(
                    new ProductError.NotFound(productId)
                ));
            }
            DisplayProduct display = displayOpt.get();

            // 재고 상품 조회
            Optional<InventoryProduct> inventoryOpt = productRepository.findInventoryProductById(productId);
            if (inventoryOpt.isEmpty()) {
                return Result.failure(new PlaceOrderError.ProductIssue(
                    new ProductError.NotFound(productId)
                ));
            }
            InventoryProduct inventory = inventoryOpt.get();

            // 재고 확인
            if (!inventory.isAvailable(item.quantity())) {
                return Result.failure(new PlaceOrderError.ProductIssue(
                    new ProductError.InsufficientStock(productId, item.quantity(), inventory.stock().value())
                ));
            }

            // 주문 라인 생성
            OrderLine orderLine = new OrderLine(
                productId,
                display.name().value(),
                new Quantity(item.quantity()),
                display.price()
            );

            result.add(new OrderLineWithProduct(orderLine, display, inventory));
        }

        return Result.success(result);
    }

    /**
     * 쿠폰 적용
     */
    private Result<UsedCoupon, PlaceOrderError> applyCoupon(String couponCode, Money orderAmount) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
        if (couponOpt.isEmpty()) {
            return Result.failure(new PlaceOrderError.CouponIssue(
                new com.ecommerce.domain.coupon.CouponError.NotFound(couponCode)
            ));
        }
        Coupon coupon = couponOpt.get();

        // 쿠폰 사용 (OrderId는 아직 없으므로 임시 ID 사용)
        OrderId tempOrderId = OrderId.generate();
        return coupon.use(tempOrderId, orderAmount)
            .mapError(error -> new PlaceOrderError.CouponIssue(error));
    }

    /**
     * 주문 라인 + 상품 정보 묶음
     */
    private record OrderLineWithProduct(
        OrderLine orderLine,
        DisplayProduct display,
        InventoryProduct inventory
    ) {}
}
