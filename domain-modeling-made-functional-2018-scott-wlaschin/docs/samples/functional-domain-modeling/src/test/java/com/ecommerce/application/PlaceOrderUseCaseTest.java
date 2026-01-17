package com.ecommerce.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.domain.coupon.*;
import com.ecommerce.domain.member.*;
import com.ecommerce.domain.order.*;
import com.ecommerce.domain.payment.*;
import com.ecommerce.domain.product.*;
import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Currency;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@DisplayName("PlaceOrderUseCase 통합 테스트")
class PlaceOrderUseCaseTest {

    private PlaceOrderUseCase useCase;
    private InMemoryMemberRepository memberRepository;
    private InMemoryProductRepository productRepository;
    private InMemoryCouponRepository couponRepository;
    private InMemoryOrderRepository orderRepository;
    private MockPaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        memberRepository = new InMemoryMemberRepository();
        productRepository = new InMemoryProductRepository();
        couponRepository = new InMemoryCouponRepository();
        orderRepository = new InMemoryOrderRepository();
        paymentGateway = new MockPaymentGateway();

        useCase = new PlaceOrderUseCase(
            memberRepository,
            productRepository,
            couponRepository,
            orderRepository,
            paymentGateway,
            new OrderDomainService()
        );

        // 테스트 데이터 설정
        setupTestData();
    }

    private void setupTestData() {
        // 회원 등록
        Member member = Member.create(
            new MemberId(1L),
            "홍길동",
            new EmailVerification.VerifiedEmail("hong@example.com", LocalDateTime.now())
        );
        memberRepository.save(member);

        // VIP 회원 등록
        Member vipMember = Member.create(
            new MemberId(2L),
            "김VIP",
            new EmailVerification.VerifiedEmail("vip@example.com", LocalDateTime.now())
        ).upgradeGrade(new MemberGrade.Vip());
        memberRepository.save(vipMember);

        // 상품 등록
        ProductId productId = new ProductId("P-1");
        productRepository.saveDisplayProduct(new DisplayProduct(
            productId,
            new ProductName("테스트 상품"),
            "상품 설명",
            List.of("image.jpg"),
            Money.krw(10000),
            Money.krw(12000),
            4.5,
            100,
            true
        ));
        productRepository.saveInventoryProduct(new InventoryProduct(
            productId,
            new StockQuantity(50),
            WarehouseLocation.of("WH-A"),
            10
        ));

        ProductId productId2 = new ProductId("P-2");
        productRepository.saveDisplayProduct(new DisplayProduct(
            productId2,
            new ProductName("다른 상품"),
            "상품 설명",
            List.of("image2.jpg"),
            Money.krw(20000),
            null,
            4.0,
            50,
            true
        ));
        productRepository.saveInventoryProduct(new InventoryProduct(
            productId2,
            new StockQuantity(3),  // 재고 적음
            WarehouseLocation.of("WH-B"),
            5
        ));

        // 쿠폰 등록
        Coupon coupon = Coupon.issue(
            new CouponId("CP-1"),
            "SUMMER2024",
            new CouponType.FixedAmount(Money.krw(3000)),
            Money.krw(20000),
            LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(coupon);

        Coupon percentCoupon = Coupon.issue(
            new CouponId("CP-2"),
            "VIP10",
            new CouponType.Percentage(10, Money.krw(10000)),
            Money.krw(50000),
            LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(percentCoupon);
    }

    @Nested
    @DisplayName("정상 주문 흐름 테스트")
    class SuccessfulOrderTests {

        @Test
        @DisplayName("쿠폰 없이 주문 성공")
        void orderWithoutCoupon() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 2)),
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 101호",
                new PaymentMethod.Points(23000), // 20,000 + 3,000 배송비
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.value().orderId());
            // 재고 확인
            InventoryProduct updated = productRepository.findInventoryProductById(new ProductId("P-1")).get();
            assertEquals(48, updated.stock().value()); // 50 - 2
        }

        @Test
        @DisplayName("쿠폰 적용하여 주문 성공")
        void orderWithCoupon() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 3)),  // 30,000원
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                null,
                new PaymentMethod.Points(30000),
                "SUMMER2024"  // 3,000원 할인
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isSuccess());
            // 쿠폰 사용 확인
            Coupon usedCoupon = couponRepository.findByCode("SUMMER2024").get();
            assertTrue(usedCoupon.status() instanceof CouponStatus.Used);
        }

        @Test
        @DisplayName("VIP 회원 무료배송 적용")
        void vipMemberFreeShipping() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                2L,  // VIP 회원
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 1)),  // 10,000원
                "김VIP",
                null,
                null,
                "서울시 강남구",
                null,
                new PaymentMethod.Points(9000), // 10% 할인 = 9,000원, 배송비 무료
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("실패 케이스 테스트")
    class FailureCasesTests {

        @Test
        @DisplayName("회원 없음")
        void memberNotFound() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                999L,  // 존재하지 않는 회원
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 1)),
                "누군가",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(13000),
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.MemberIssue);
        }

        @Test
        @DisplayName("상품 없음")
        void productNotFound() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("NOT-EXIST", 1)),
                "홍길동",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(10000),
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.ProductIssue);
        }

        @Test
        @DisplayName("재고 부족")
        void insufficientStock() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-2", 10)),  // 재고 3개뿐
                "홍길동",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(200000),
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.ProductIssue);
            PlaceOrderError.ProductIssue productIssue = (PlaceOrderError.ProductIssue) result.error();
            assertTrue(productIssue.error() instanceof ProductError.InsufficientStock);
        }

        @Test
        @DisplayName("쿠폰 없음")
        void couponNotFound() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 2)),
                "홍길동",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(23000),
                "INVALID-COUPON"
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.CouponIssue);
        }

        @Test
        @DisplayName("쿠폰 최소 주문 금액 미달")
        void couponMinOrderNotMet() {
            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 1)),  // 10,000원
                "홍길동",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(13000),
                "SUMMER2024"  // 최소 20,000원 필요
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.CouponIssue);
        }

        @Test
        @DisplayName("결제 실패")
        void paymentFailed() {
            paymentGateway.setAlwaysFail(true);

            PlaceOrderCommand command = new PlaceOrderCommand(
                1L,
                List.of(new PlaceOrderCommand.OrderLineItem("P-1", 2)),
                "홍길동",
                null,
                null,
                "서울시",
                null,
                new PaymentMethod.Points(23000),
                null
            );

            Result<OrderPlaced, PlaceOrderError> result = useCase.execute(command);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof PlaceOrderError.PaymentIssue);
        }
    }

    @Nested
    @DisplayName("OrderDomainService 테스트")
    class OrderDomainServiceTests {

        private OrderDomainService domainService;

        @BeforeEach
        void setUp() {
            domainService = new OrderDomainService();
        }

        @Test
        @DisplayName("소계 계산")
        void calculateSubtotal() {
            List<OrderLine> lines = List.of(
                new OrderLine(new ProductId("P-1"), new Quantity(2), Money.krw(10000)),
                new OrderLine(new ProductId("P-2"), new Quantity(1), Money.krw(20000))
            );

            Money subtotal = domainService.calculateSubtotal(lines);

            assertEquals(BigDecimal.valueOf(40000), subtotal.amount());
        }

        @Test
        @DisplayName("등급 할인 계산")
        void calculateGradeDiscount() {
            Money subtotal = Money.krw(100000);
            MemberGrade grade = new MemberGrade.Vip();  // 10% 할인

            Money discount = domainService.calculateGradeDiscount(subtotal, grade);

            assertEquals(0, BigDecimal.valueOf(10000).compareTo(discount.amount()));
        }

        @Test
        @DisplayName("무료배송 조건 - VIP")
        void freeShipping_vip() {
            Member vipMember = Member.create(
                new MemberId(1L),
                "VIP",
                new EmailVerification.VerifiedEmail("vip@test.com", LocalDateTime.now())
            ).upgradeGrade(new MemberGrade.Vip());

            Money shippingFee = domainService.calculateShippingFee(Money.krw(10000), vipMember);

            assertEquals(BigDecimal.ZERO, shippingFee.amount());
        }

        @Test
        @DisplayName("무료배송 조건 - 5만원 이상")
        void freeShipping_overThreshold() {
            Member member = Member.create(
                new MemberId(1L),
                "일반",
                new EmailVerification.VerifiedEmail("test@test.com", LocalDateTime.now())
            );

            Money shippingFee = domainService.calculateShippingFee(Money.krw(50000), member);

            assertEquals(BigDecimal.ZERO, shippingFee.amount());
        }

        @Test
        @DisplayName("배송비 부과 - 일반 회원 5만원 미만")
        void shippingFee_charged() {
            Member member = Member.create(
                new MemberId(1L),
                "일반",
                new EmailVerification.VerifiedEmail("test@test.com", LocalDateTime.now())
            );

            Money shippingFee = domainService.calculateShippingFee(Money.krw(30000), member);

            assertEquals(BigDecimal.valueOf(3000), shippingFee.amount());
        }
    }

    // === Mock Repository 구현 ===

    static class InMemoryMemberRepository implements MemberRepository {
        private final Map<MemberId, Member> store = new HashMap<>();

        @Override
        public Optional<Member> findById(MemberId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            return store.values().stream()
                .filter(m -> m.emailAddress().equals(email))
                .findFirst();
        }

        @Override
        public Member save(Member member) {
            store.put(member.id(), member);
            return member;
        }

        @Override
        public void delete(MemberId id) {
            store.remove(id);
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(m -> m.emailAddress().equals(email));
        }
    }

    static class InMemoryProductRepository implements ProductRepository {
        private final Map<ProductId, DisplayProduct> displayStore = new HashMap<>();
        private final Map<ProductId, InventoryProduct> inventoryStore = new HashMap<>();
        private final Map<ProductId, SettlementProduct> settlementStore = new HashMap<>();

        @Override
        public Optional<DisplayProduct> findDisplayProductById(ProductId id) {
            return Optional.ofNullable(displayStore.get(id));
        }

        @Override
        public Optional<InventoryProduct> findInventoryProductById(ProductId id) {
            return Optional.ofNullable(inventoryStore.get(id));
        }

        @Override
        public Optional<SettlementProduct> findSettlementProductById(ProductId id) {
            return Optional.ofNullable(settlementStore.get(id));
        }

        @Override
        public List<DisplayProduct> findDisplayProductsByIds(List<ProductId> ids) {
            return ids.stream()
                .map(displayStore::get)
                .filter(Objects::nonNull)
                .toList();
        }

        @Override
        public List<InventoryProduct> findInventoryProductsByIds(List<ProductId> ids) {
            return ids.stream()
                .map(inventoryStore::get)
                .filter(Objects::nonNull)
                .toList();
        }

        @Override
        public InventoryProduct saveInventoryProduct(InventoryProduct product) {
            inventoryStore.put(product.id(), product);
            return product;
        }

        public void saveDisplayProduct(DisplayProduct product) {
            displayStore.put(product.id(), product);
        }

        @Override
        public boolean existsById(ProductId id) {
            return displayStore.containsKey(id);
        }
    }

    static class InMemoryCouponRepository implements CouponRepository {
        private final Map<CouponId, Coupon> store = new HashMap<>();

        @Override
        public Optional<Coupon> findById(CouponId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Coupon> findByCode(String code) {
            return store.values().stream()
                .filter(c -> c.code().equals(code))
                .findFirst();
        }

        @Override
        public Coupon save(Coupon coupon) {
            store.put(coupon.id(), coupon);
            return coupon;
        }

        @Override
        public void delete(CouponId id) {
            store.remove(id);
        }
    }

    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<OrderId, Order> store = new HashMap<>();

        @Override
        public Optional<Order> findById(OrderId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Order> findByCustomerId(CustomerId customerId) {
            return store.values().stream()
                .filter(o -> o.customerId().equals(customerId))
                .toList();
        }

        @Override
        public Order save(Order order) {
            store.put(order.id(), order);
            return order;
        }

        @Override
        public void delete(OrderId id) {
            store.remove(id);
        }
    }

    static class MockPaymentGateway implements PaymentGateway {
        private boolean alwaysFail = false;

        public void setAlwaysFail(boolean alwaysFail) {
            this.alwaysFail = alwaysFail;
        }

        @Override
        public Result<String, PaymentError> charge(Money amount, PaymentMethod method) {
            if (alwaysFail) {
                return Result.failure(new PaymentError.SystemError("결제 실패"));
            }
            return Result.success("TX-" + System.currentTimeMillis());
        }

        @Override
        public Result<String, PaymentError> refund(String transactionId, Money amount) {
            return Result.success("REFUND-" + System.currentTimeMillis());
        }
    }
}
