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
 * 주문 도메인 서비스 (Functional Core)
 * 순수 비즈니스 로직만 포함, 부수효과 없음
 */
public class OrderDomainService {

    private static final Money DEFAULT_SHIPPING_FEE = Money.krw(3000);
    private static final Money FREE_SHIPPING_THRESHOLD = Money.krw(50000);

    /**
     * 소계 계산
     */
    public Money calculateSubtotal(List<OrderLine> lines) {
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
        Money subtotal = calculateSubtotal(lines);
        Money gradeDiscount = calculateGradeDiscount(subtotal, member.grade());
        Money couponDiscount = couponType != null
            ? calculateCouponDiscount(subtotal, couponType)
            : Money.ZERO;
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
