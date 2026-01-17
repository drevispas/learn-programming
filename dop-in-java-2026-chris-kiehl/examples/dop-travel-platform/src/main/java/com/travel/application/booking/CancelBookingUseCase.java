package com.travel.application.booking;

import com.travel.domain.booking.*;
import com.travel.shared.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 취소 UseCase - Imperative Shell
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 취소 워크플로우를 조율하며 부수효과(I/O)를 관리
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core / Imperative Shell</h2>
 * <pre>
 * [IS] UseCase 역할:
 * 1. Top Bun: 예약 조회 (I/O)
 * 2. Meat: BookingDomainService.cancelBooking() 호출 (FC)
 * 3. Bottom Bun: 예약 저장, 환불 처리 (I/O)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Result 체이닝</h2>
 * <pre>
 * loadBooking(bookingId)
 *     .flatMap(booking -> BookingDomainService.cancelBooking(booking, reason, cancelledBy))
 *     .map(this::saveAndProcessRefund)
 * </pre>
 */
@Service
public class CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    // private final PaymentGateway paymentGateway; // 환불 처리용 (TODO)

    public CancelBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * 사용자에 의한 예약 취소
     *
     * @param bookingId 취소할 예약 ID
     * @param reason    취소 사유
     * @return 취소된 예약 또는 오류
     */
    @Transactional
    public Result<Booking, BookingError> cancelByUser(BookingId bookingId, String reason) {
        return execute(bookingId, reason, BookingStatus.Cancelled.CancelledBy.USER);
    }

    /**
     * 시스템에 의한 예약 취소 (결제 만료 등)
     *
     * @param bookingId 취소할 예약 ID
     * @param reason    취소 사유
     * @return 취소된 예약 또는 오류
     */
    @Transactional
    public Result<Booking, BookingError> cancelBySystem(BookingId bookingId, String reason) {
        return execute(bookingId, reason, BookingStatus.Cancelled.CancelledBy.SYSTEM);
    }

    /**
     * 관리자에 의한 예약 취소
     *
     * @param bookingId 취소할 예약 ID
     * @param reason    취소 사유
     * @return 취소된 예약 또는 오류
     */
    @Transactional
    public Result<Booking, BookingError> cancelByAdmin(BookingId bookingId, String reason) {
        return execute(bookingId, reason, BookingStatus.Cancelled.CancelledBy.ADMIN);
    }

    // ============================================
    // [Key Point] 메인 실행 로직 - 샌드위치 구조
    // ============================================

    private Result<Booking, BookingError> execute(
            BookingId bookingId,
            String reason,
            BookingStatus.Cancelled.CancelledBy cancelledBy
    ) {
        // ========================================
        // [Top Bun] 데이터 조회 (Imperative Shell)
        // ========================================

        // 1. 예약 조회
        Result<Booking, BookingError> bookingResult = bookingRepository.findById(bookingId);
        if (bookingResult.isFailure()) {
            return bookingResult;
        }
        Booking booking = bookingResult.getOrThrow();

        // ========================================
        // [Meat] 순수 비즈니스 로직 (Functional Core)
        // ========================================

        // 2. [FC] 취소 가능 여부 검증 및 취소 처리
        Result<Booking, BookingError> cancelResult =
                BookingDomainService.cancelBooking(booking, reason, cancelledBy);
        if (cancelResult.isFailure()) {
            return cancelResult;
        }
        Booking cancelledBooking = cancelResult.getOrThrow();

        // ========================================
        // [Bottom Bun] 부수효과 실행 (Imperative Shell)
        // ========================================

        // 3. [IS] 예약 저장
        Booking savedBooking = bookingRepository.save(cancelledBooking);

        // 4. [IS] 환불 처리 (확정된 예약이었던 경우)
        if (savedBooking.status() instanceof BookingStatus.Cancelled cancelled) {
            if (cancelled.hasRefund()) {
                processRefund(savedBooking, cancelled);
            }
        }

        // 5. [IS] 쿠폰 복구 (쿠폰 적용되었던 경우)
        if (booking.hasCoupon()) {
            restoreCoupon(booking.couponId());
        }

        // 6. [IS] 알림 발송 (TODO)
        // notificationService.sendCancellationNotification(savedBooking);

        return Result.success(savedBooking);
    }

    // ============================================
    // [Bottom Bun] 부수효과 메서드
    // ============================================

    /**
     * 환불 처리 (I/O)
     */
    private void processRefund(Booking booking, BookingStatus.Cancelled cancelled) {
        // TODO: PaymentGateway를 통한 실제 환불 처리
        // paymentGateway.refund(cancelled.refundAmount(), booking.id());
        System.out.println("[IS] 환불 처리: " + cancelled.refundAmount() + " for booking " + booking.id());
    }

    /**
     * 쿠폰 복구 (I/O)
     */
    private void restoreCoupon(String couponId) {
        // TODO: CouponRepository를 통한 쿠폰 복구
        // couponRepository.findById(couponId).ifPresent(coupon -> {
        //     couponRepository.save(coupon.restore());
        // });
        System.out.println("[IS] 쿠폰 복구: " + couponId);
    }
}
