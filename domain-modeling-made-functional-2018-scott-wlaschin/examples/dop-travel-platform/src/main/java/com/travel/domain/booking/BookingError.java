package com.travel.domain.booking;

import com.travel.shared.types.Money;

import java.time.Instant;
import java.util.List;

/**
 * 예약 도메인 오류 - Error as Data
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 관련 비즈니스 오류를 타입으로 명시적 표현
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Error as Data</h2>
 * <pre>
 * [Key Point] 예외 대신 값으로 오류 표현:
 *
 * [Before] 예외 사용:
 *   throw new BookingNotFoundException(id);
 *   throw new InvalidStatusException("이미 취소됨");
 *   → catch 블록 필수, 처리 누락 가능
 *
 * [After] sealed interface 사용:
 *   Result&lt;Booking, BookingError&gt; findById(BookingId id);
 *   → 컴파일러가 모든 오류 케이스 처리 강제
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 각 오류 케이스가 해당 오류에 필요한 정보만 보유:
 *
 * NotFound: 찾으려던 ID
 * InvalidStatus: 현재 상태, 요청한 작업
 * ItemUnavailable: 이용 불가한 항목 정보
 * PaymentRequired: 결제 필요 금액
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] 오류 메시지만 String으로 → 프로그래밍적 처리 어려움</li>
 *   <li>[Trap] 모든 오류를 하나의 Exception으로 → 구체적 처리 불가</li>
 *   <li>[Why sealed] 가능한 오류 케이스를 명시적으로 제한</li>
 * </ul>
 */
public sealed interface BookingError permits
        BookingError.NotFound,
        BookingError.InvalidStatus,
        BookingError.ItemUnavailable,
        BookingError.PaymentRequired,
        BookingError.CancellationNotAllowed,
        BookingError.CouponNotApplicable,
        BookingError.InsufficientStock,
        BookingError.ValidationFailed {

    // ============================================
    // [Key Point] 공통 인터페이스
    // ============================================

    /**
     * 사용자에게 표시할 메시지
     */
    String message();

    /**
     * 오류 코드 (API 응답, 로깅용)
     */
    String code();

    // ============================================
    // 각 오류 케이스 - 해당 오류에 필요한 정보 보유
    // ============================================

    /**
     * 예약을 찾을 수 없음
     *
     * @param bookingId 찾으려던 예약 ID
     */
    record NotFound(BookingId bookingId) implements BookingError {
        @Override
        public String message() {
            return "예약을 찾을 수 없습니다: " + bookingId;
        }

        @Override
        public String code() {
            return "BOOKING_NOT_FOUND";
        }
    }

    /**
     * 현재 상태에서 요청한 작업 불가
     *
     * @param currentStatus 현재 상태
     * @param requestedAction 요청한 작업
     */
    record InvalidStatus(
            BookingStatus currentStatus,
            String requestedAction
    ) implements BookingError {
        @Override
        public String message() {
            String statusName = switch (currentStatus) {
                case BookingStatus.Pending p -> "결제 대기 중";
                case BookingStatus.Confirmed c -> "확정됨";
                case BookingStatus.Cancelled c -> "취소됨";
                case BookingStatus.Completed c -> "완료됨";
                case BookingStatus.NoShow n -> "노쇼";
            };
            return statusName + " 상태에서는 " + requestedAction + " 작업을 수행할 수 없습니다";
        }

        @Override
        public String code() {
            return "INVALID_BOOKING_STATUS";
        }
    }

    /**
     * 예약 항목 이용 불가
     *
     * @param itemName 항목 이름
     * @param reason   이용 불가 사유
     */
    record ItemUnavailable(
            String itemName,
            String reason
    ) implements BookingError {
        @Override
        public String message() {
            return itemName + " 항목을 예약할 수 없습니다: " + reason;
        }

        @Override
        public String code() {
            return "ITEM_UNAVAILABLE";
        }
    }

    /**
     * 결제 필요
     *
     * @param requiredAmount 필요 금액
     * @param expiresAt      결제 만료 시간
     */
    record PaymentRequired(
            Money requiredAmount,
            Instant expiresAt
    ) implements BookingError {
        @Override
        public String message() {
            return requiredAmount.formatted() + " 결제가 필요합니다. 만료: " + expiresAt;
        }

        @Override
        public String code() {
            return "PAYMENT_REQUIRED";
        }
    }

    /**
     * 취소 불가
     *
     * @param reason 취소 불가 사유
     */
    record CancellationNotAllowed(String reason) implements BookingError {
        @Override
        public String message() {
            return "예약 취소가 불가능합니다: " + reason;
        }

        @Override
        public String code() {
            return "CANCELLATION_NOT_ALLOWED";
        }
    }

    /**
     * 쿠폰 적용 불가
     *
     * @param couponId 쿠폰 ID
     * @param reason   적용 불가 사유
     */
    record CouponNotApplicable(
            String couponId,
            String reason
    ) implements BookingError {
        @Override
        public String message() {
            return "쿠폰(" + couponId + ")을 적용할 수 없습니다: " + reason;
        }

        @Override
        public String code() {
            return "COUPON_NOT_APPLICABLE";
        }
    }

    /**
     * 재고 부족
     *
     * @param productId 상품 ID
     * @param requestedQuantity 요청 수량
     * @param availableQuantity 가용 수량
     */
    record InsufficientStock(
            String productId,
            int requestedQuantity,
            int availableQuantity
    ) implements BookingError {
        @Override
        public String message() {
            return "재고가 부족합니다. 요청: " + requestedQuantity + ", 가용: " + availableQuantity;
        }

        @Override
        public String code() {
            return "INSUFFICIENT_STOCK";
        }
    }

    /**
     * 유효성 검증 실패
     *
     * @param violations 검증 실패 항목들
     */
    record ValidationFailed(List<String> violations) implements BookingError {
        public ValidationFailed {
            if (violations == null || violations.isEmpty()) {
                throw new IllegalArgumentException("검증 실패 항목은 최소 1개 이상 필요합니다");
            }
            violations = List.copyOf(violations);
        }

        @Override
        public String message() {
            return "유효성 검증 실패: " + String.join(", ", violations);
        }

        @Override
        public String code() {
            return "VALIDATION_FAILED";
        }
    }
}
