package com.travel.application.booking;

import com.travel.domain.booking.*;
import com.travel.shared.Result;
import com.travel.shared.types.Money;

/**
 * 예약 도메인 서비스 - 순수 비즈니스 로직 (Functional Core)
 *
 * <h2>목적 (Purpose)</h2>
 * 여러 도메인 객체를 조합하는 복잡한 비즈니스 로직을 순수 함수로 제공
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core</h2>
 * <pre>
 * [FC] BookingDomainService의 특징:
 * - 모든 메서드가 순수 함수
 * - 외부 의존성 없음 (Repository, Gateway 호출 안 함)
 * - I/O 없음
 * - 동일 입력 → 동일 출력 보장
 *
 * [Why 도메인 서비스]
 * - 단일 엔티티(Booking)에 속하지 않는 비즈니스 로직
 * - 여러 도메인 객체 조합 필요
 * - 하지만 여전히 순수 함수로 작성 가능
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 1 데이터와 동작 분리</h2>
 * <pre>
 * Booking (record) = 순수 데이터
 * BookingCalculations = 단순 계산 함수
 * BookingDomainService = 복잡한 비즈니스 규칙 (여전히 순수)
 * CreateBookingUseCase = 부수효과 조율 (Imperative Shell)
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] 도메인 서비스에서 Repository 호출 → FC 아님, UseCase에서 해야 함</li>
 *   <li>[Trap] 도메인 서비스에 상태 유지 → 순수성 위반</li>
 *   <li>[Why Result] 예외 대신 Result 반환 → 함수 전체성 보장</li>
 * </ul>
 */
public final class BookingDomainService {

    // 인스턴스화 방지
    private BookingDomainService() {}

    // ============================================
    // [Key Point] 할인 적용 로직 - 순수 함수
    // ============================================

    /**
     * 쿠폰 할인 적용
     *
     * <pre>
     * [FC] 순수 함수:
     * - 입력: Booking, 할인 금액, 쿠폰 ID
     * - 출력: Result&lt;Booking, BookingError&gt;
     * - 부수효과: 없음
     *
     * [Key Point] 쿠폰 유효성 검증은 UseCase(IS)에서 이미 수행
     * 이 함수는 검증된 할인 금액을 적용하는 것만 담당
     * </pre>
     *
     * @param booking   원본 예약
     * @param discount  할인 금액 (이미 검증됨)
     * @param couponId  쿠폰 ID
     * @return 할인 적용된 예약
     */
    public static Result<Booking, BookingError> applyDiscount(
            Booking booking,
            Money discount,
            String couponId
    ) {
        // 이미 쿠폰이 적용된 경우
        if (booking.hasCoupon()) {
            return Result.failure(new BookingError.CouponNotApplicable(
                    couponId,
                    "이미 다른 쿠폰이 적용되어 있습니다"
            ));
        }

        // 할인 금액이 총 금액을 초과하는 경우
        if (discount.isGreaterThan(booking.totalAmount())) {
            return Result.failure(new BookingError.CouponNotApplicable(
                    couponId,
                    "할인 금액이 총 금액을 초과합니다"
            ));
        }

        // [Ch 2] Wither 패턴으로 새 Booking 반환
        Booking discountedBooking = booking.withDiscount(discount, couponId);
        return Result.success(discountedBooking);
    }

    // ============================================
    // [Key Point] 상태 전이 검증 로직 - 순수 함수
    // ============================================

    /**
     * 예약 확정 가능 여부 검증 및 확정
     *
     * <pre>
     * [FC] 순수 함수:
     * - 상태 전이 규칙 검증
     * - 결제 ID 유효성 검증
     * - 새 Booking 반환
     * </pre>
     *
     * @param booking   원본 예약
     * @param paymentId 결제 ID
     * @return 확정된 예약
     */
    public static Result<Booking, BookingError> confirmBooking(
            Booking booking,
            String paymentId
    ) {
        // Pending 상태가 아닌 경우
        if (!(booking.status() instanceof BookingStatus.Pending)) {
            return Result.failure(new BookingError.InvalidStatus(
                    booking.status(),
                    "예약 확정"
            ));
        }

        // 결제 대기 시간 만료
        BookingStatus.Pending pending = (BookingStatus.Pending) booking.status();
        if (pending.isExpired()) {
            return Result.failure(new BookingError.PaymentRequired(
                    booking.finalAmount(),
                    pending.expiresAt()
            ));
        }

        // 결제 ID 검증
        if (paymentId == null || paymentId.isBlank()) {
            return Result.failure(new BookingError.ValidationFailed(
                    java.util.List.of("결제 ID는 필수입니다")
            ));
        }

        // [Ch 2] Wither 패턴으로 상태 전이
        Booking confirmedBooking = booking.confirm(paymentId);
        return Result.success(confirmedBooking);
    }

    /**
     * 예약 취소 가능 여부 검증 및 취소
     *
     * @param booking     원본 예약
     * @param reason      취소 사유
     * @param cancelledBy 취소 주체
     * @return 취소된 예약
     */
    public static Result<Booking, BookingError> cancelBooking(
            Booking booking,
            String reason,
            BookingStatus.Cancelled.CancelledBy cancelledBy
    ) {
        // 취소 불가 상태 확인
        if (!booking.status().isCancellable()) {
            return Result.failure(new BookingError.InvalidStatus(
                    booking.status(),
                    "예약 취소"
            ));
        }

        // 취소 사유 검증
        if (reason == null || reason.isBlank()) {
            return Result.failure(new BookingError.ValidationFailed(
                    java.util.List.of("취소 사유는 필수입니다")
            ));
        }

        // 환불 금액 계산 (확정된 예약만)
        Money refundAmount = null;
        if (booking.isConfirmed()) {
            refundAmount = calculateRefundForCancellation(booking);
        }

        // 취소 처리
        Booking cancelledBooking = booking.cancel(reason, refundAmount, cancelledBy);
        return Result.success(cancelledBooking);
    }

    /**
     * 취소 시 환불 금액 계산
     *
     * <p>[FC] 순수 함수: 비즈니스 규칙에 따른 환불 금액 결정</p>
     */
    private static Money calculateRefundForCancellation(Booking booking) {
        // 실제로는 이용일까지 남은 일수에 따라 환불율 결정
        // 여기서는 단순화하여 전액 환불
        return booking.finalAmount();
    }

    // ============================================
    // [Key Point] 복잡한 비즈니스 규칙 - 순수 함수
    // ============================================

    /**
     * 예약 항목 추가 가능 여부 검증 및 추가
     *
     * @param booking 원본 예약
     * @param item    추가할 항목
     * @return 항목 추가된 예약
     */
    public static Result<Booking, BookingError> addItem(
            Booking booking,
            BookingItem item
    ) {
        // Pending 상태에서만 항목 추가 가능
        if (!booking.isPending()) {
            return Result.failure(new BookingError.InvalidStatus(
                    booking.status(),
                    "항목 추가"
            ));
        }

        // 최대 항목 수 검증
        if (booking.itemCount() >= 10) {
            return Result.failure(new BookingError.ValidationFailed(
                    java.util.List.of("예약 항목은 최대 10개까지 가능합니다")
            ));
        }

        // 중복 상품 검증
        boolean isDuplicate = booking.items().stream()
                .anyMatch(existing -> existing.productId().equals(item.productId()));
        if (isDuplicate) {
            return Result.failure(new BookingError.ValidationFailed(
                    java.util.List.of("이미 포함된 상품입니다: " + item.productId())
            ));
        }

        // [Ch 2] Wither 패턴으로 항목 추가
        Booking updatedBooking = booking.withAddedItem(item);
        return Result.success(updatedBooking);
    }

    /**
     * 예약 완료 처리
     *
     * @param booking 원본 예약
     * @return 완료된 예약
     */
    public static Result<Booking, BookingError> completeBooking(Booking booking) {
        // Confirmed 상태에서만 완료 가능
        if (!booking.isConfirmed()) {
            return Result.failure(new BookingError.InvalidStatus(
                    booking.status(),
                    "이용 완료"
            ));
        }

        Booking completedBooking = booking.complete();
        return Result.success(completedBooking);
    }

    /**
     * 노쇼 처리
     *
     * @param booking 원본 예약
     * @return 노쇼 처리된 예약
     */
    public static Result<Booking, BookingError> markAsNoShow(Booking booking) {
        // Confirmed 상태에서만 노쇼 처리 가능
        if (!booking.isConfirmed()) {
            return Result.failure(new BookingError.InvalidStatus(
                    booking.status(),
                    "노쇼 처리"
            ));
        }

        // 노쇼 패널티 계산 (30%)
        Money penalty = BookingCalculations.calculateNoShowPenalty(booking.finalAmount());

        Booking noShowBooking = booking.markNoShow(penalty);
        return Result.success(noShowBooking);
    }
}
