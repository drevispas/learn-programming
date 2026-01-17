package com.ecommerce.application;

import com.ecommerce.domain.coupon.CouponType;
import com.ecommerce.domain.member.Member;
import com.ecommerce.domain.member.MemberGrade;
import com.ecommerce.domain.order.CustomerId;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.domain.order.OrderLine;
import com.ecommerce.domain.order.ShippingAddress;
import com.ecommerce.shared.types.Money;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 도메인 서비스 - Functional Core (Chapter 9)
 *
 * <h2>목적 (Purpose)</h2>
 * 순수 비즈니스 로직만 포함한다. 외부 의존성(Repository, Gateway 등)이 없으며,
 * 모든 메서드가 입력 → 출력만 있는 순수 함수다.
 *
 * <h2>핵심 개념 (Key Concept): 순수 함수의 이점</h2>
 * <ul>
 *   <li><b>테스트 용이성</b>: Mock 객체 없이 입력/출력만으로 테스트</li>
 *   <li><b>예측 가능성</b>: 동일 입력에 항상 동일 출력 (참조 투명성)</li>
 *   <li><b>병렬 처리 안전</b>: 상태 변경 없으므로 동시성 문제 없음</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // 단위 테스트 - Mock 불필요, 입력/출력만 검증
 * @Test
 * void calculateSubtotal_shouldSumAllLines() {
 *     var service = new OrderDomainService();
 *     var lines = List.of(
 *         new OrderLine(..., new Quantity(2), Money.krw(1000)),
 *         new OrderLine(..., new Quantity(3), Money.krw(2000))
 *     );
 *
 *     Money result = service.calculateSubtotal(lines);
 *
 *     assertEquals(Money.krw(8000), result);  // 2*1000 + 3*2000
 * }
 * }</pre>
 *
 * @see PlaceOrderUseCase Imperative Shell - 외부 의존성 조율
 */
public class OrderDomainService {

    private static final Money DEFAULT_SHIPPING_FEE = Money.krw(3000);
    private static final Money FREE_SHIPPING_THRESHOLD = Money.krw(50000);

    /**
     * 소계 계산
     */
    public Money calculateSubtotal(List<OrderLine> lines) {
        // [Pure Function] 입력만으로 출력 결정, 외부 상태 참조 없음
        // [Testability] Mock 없이 값만으로 테스트 가능
        // [Composition] reduce로 함수 합성 - 각 OrderLine의 subtotal을 누적
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    /**
     * 등급 할인 계산
     */
    public Money calculateGradeDiscount(Money subtotal, MemberGrade grade) {
        if (grade.discountRate() == 0) {
            return Money.ZERO;
        }
        return subtotal.multiply(grade.discountRate()).divide(100);
    }

    /**
     * 쿠폰 할인 계산
     */
    public Money calculateCouponDiscount(Money subtotal, CouponType couponType) {
        return couponType.calculateDiscount(subtotal);
    }

    /**
     * 배송비 계산
     */
    public Money calculateShippingFee(Money subtotal, Member member) {
        // VIP는 무료배송
        if (member.canGetFreeShipping()) {
            return Money.ZERO;
        }
        // 5만원 이상 무료배송
        if (subtotal.isGreaterThanOrEqual(FREE_SHIPPING_THRESHOLD)) {
            return Money.ZERO;
        }
        return DEFAULT_SHIPPING_FEE;
    }

    /**
     * 최종 금액 계산
     */
    public Money calculateTotalAmount(Money subtotal, Money discount, Money shippingFee) {
        return subtotal.subtract(discount).add(shippingFee);
    }

    /**
     * 주문 생성
     */
    public Order createOrder(
        CustomerId customerId,
        List<OrderLine> lines,
        ShippingAddress shippingAddress,
        Money discount,
        Money shippingFee
    ) {
        // [Pure Function] 입력 데이터로 새 Order 생성
        // [Immutable] 생성된 Order는 불변 - 상태 변경 시 새 인스턴스 생성
        return Order.create(
            OrderId.generate(),
            customerId,
            lines,
            shippingAddress,
            discount,
            shippingFee,
            LocalDateTime.now().plusDays(1) // 결제 기한: 1일
        );
    }

    /**
     * 종합 가격 계산 결과
     */
    public record PricingResult(
        Money subtotal,
        Money gradeDiscount,
        Money couponDiscount,
        Money totalDiscount,
        Money shippingFee,
        Money totalAmount
    ) {}

    /**
     * 종합 가격 계산
     */
    public PricingResult calculatePricing(
        List<OrderLine> lines,
        Member member,
        CouponType couponType
    ) {
        // [Pure Function Composition] 여러 순수 함수를 합성하여 최종 결과 계산
        // 각 단계가 순수 함수이므로 전체도 순수 함수
        Money subtotal = calculateSubtotal(lines);
        Money gradeDiscount = calculateGradeDiscount(subtotal, member.grade());
        Money couponDiscount = couponType != null
            ? calculateCouponDiscount(subtotal, couponType)
            : Money.ZERO;
        // [Immutable] Money.add()는 새 Money 반환, 원본 불변
        Money totalDiscount = gradeDiscount.add(couponDiscount);
        Money shippingFee = calculateShippingFee(subtotal, member);
        Money totalAmount = calculateTotalAmount(subtotal, totalDiscount, shippingFee);

        return new PricingResult(
            subtotal,
            gradeDiscount,
            couponDiscount,
            totalDiscount,
            shippingFee,
            totalAmount
        );
    }

    /**
     * 종합 가격 계산 (쿠폰 없음)
     */
    public PricingResult calculatePricing(List<OrderLine> lines, Member member) {
        return calculatePricing(lines, member, null);
    }
}
