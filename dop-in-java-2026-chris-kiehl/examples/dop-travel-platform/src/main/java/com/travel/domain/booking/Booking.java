package com.travel.domain.booking;

import com.travel.domain.member.MemberId;
import com.travel.shared.types.Money;

import java.time.Instant;
import java.util.List;

/**
 * 예약 - 핵심 도메인 Aggregate Root
 *
 * <h2>목적 (Purpose)</h2>
 * 여행 상품 예약의 전체 라이프사이클을 관리하는 불변 엔티티
 *
 * <h2>핵심 개념 (Key Concept): Ch 1 DOP 4대 원칙</h2>
 * <pre>
 * [Key Point] God Class 분해:
 *
 * [Before] OOP God Class:
 *   class Booking {
 *       // 데이터
 *       private String id;
 *       private List&lt;BookingItem&gt; items;
 *       private String status; // 문자열로 상태 관리
 *       private BigDecimal total;
 *       // 비즈니스 로직
 *       public void addItem(...) { ... }
 *       public void calculateTotal() { ... }
 *       public void confirm() { ... }
 *       public void cancel() { ... }
 *       public void processPayment() { ... }
 *       // 부수효과 (이메일, 알림)
 *       public void sendConfirmationEmail() { ... }
 *   }
 *
 * [After] DOP 분리:
 *   - Booking (record): 순수 데이터
 *   - BookingCalculations: 순수 계산 함수
 *   - CreateBookingUseCase: 부수효과 처리 (Imperative Shell)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Wither 패턴</h2>
 * <pre>
 * [Key Point] Record는 불변이므로 "수정"은 새 객체 생성:
 *   booking.withStatus(newStatus)  // 새 Booking 반환
 *   booking.withDiscount(discount) // 새 Booking 반환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Identity vs Value</h2>
 * <pre>
 * [Key Point] Booking은 Entity (Identity 기반):
 *   - 같은 ID면 같은 예약 (내용이 달라도)
 *   - 상태가 변해도 동일한 예약
 *
 * vs BookingItem, Money는 Value Object (Value 기반):
 *   - 내용이 같으면 같은 것
 * </pre>
 *
 * @param id           예약 ID (Identity)
 * @param memberId     회원 ID
 * @param items        예약 항목 목록 (불변 리스트)
 * @param status       예약 상태 (Sum Type)
 * @param totalAmount  총 금액
 * @param discountAmount 할인 금액
 * @param finalAmount  최종 결제 금액
 * @param couponId     적용된 쿠폰 ID (null이면 쿠폰 미적용)
 * @param createdAt    생성 시간
 * @param updatedAt    마지막 수정 시간
 */
public record Booking(
        BookingId id,
        MemberId memberId,
        List<BookingItem> items,
        BookingStatus status,
        Money totalAmount,
        Money discountAmount,
        Money finalAmount,
        String couponId,
        Instant createdAt,
        Instant updatedAt
) {

    // ============================================
    // [Key Point] Compact Constructor - 생성 시점 검증
    // Ch 2: Record의 자체 검증 기능
    // ============================================

    public Booking {
        // 필수 값 검증
        if (id == null) throw new IllegalArgumentException("예약 ID는 필수입니다");
        if (memberId == null) throw new IllegalArgumentException("회원 ID는 필수입니다");
        if (status == null) throw new IllegalArgumentException("예약 상태는 필수입니다");
        if (totalAmount == null) throw new IllegalArgumentException("총 금액은 필수입니다");
        if (finalAmount == null) throw new IllegalArgumentException("최종 금액은 필수입니다");
        if (createdAt == null) throw new IllegalArgumentException("생성 시간은 필수입니다");
        if (updatedAt == null) throw new IllegalArgumentException("수정 시간은 필수입니다");

        // [Ch 2] 불변 리스트로 방어적 복사
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("예약 항목은 최소 1개 이상 필요합니다");
        }
        items = List.copyOf(items);

        // 할인 금액 기본값
        if (discountAmount == null) {
            discountAmount = Money.zero(totalAmount.currency());
        }

        // [Ch 4] 금액 일관성 검증
        Money expected = totalAmount.subtract(discountAmount);
        if (!expected.equals(finalAmount)) {
            throw new IllegalArgumentException(
                    "금액 불일치: " + totalAmount + " - " + discountAmount + " != " + finalAmount);
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 새 예약 생성
     *
     * @param memberId 회원 ID
     * @param items    예약 항목들
     * @return 새 Booking (Pending 상태)
     */
    public static Booking create(MemberId memberId, List<BookingItem> items) {
        // [FC] 순수 계산: 총 금액 계산
        Money totalAmount = BookingCalculations.calculateTotalAmount(items);

        Instant now = Instant.now();
        return new Booking(
                BookingId.generate(),
                memberId,
                items,
                BookingStatus.Pending.withDefaultExpiry(),
                totalAmount,
                Money.zero(totalAmount.currency()),
                totalAmount,
                null,
                now,
                now
        );
    }

    // ============================================
    // [Key Point] Wither 패턴 메서드
    // Ch 2: 불변 객체의 "수정"은 새 객체 반환
    // ============================================

    /**
     * 상태 변경 (새 Booking 반환)
     *
     * <p>[Key Point] 원본 Booking은 변경되지 않음</p>
     */
    public Booking withStatus(BookingStatus newStatus) {
        return new Booking(
                id,
                memberId,
                items,
                newStatus,
                totalAmount,
                discountAmount,
                finalAmount,
                couponId,
                createdAt,
                Instant.now() // [Key Point] updatedAt 자동 갱신
        );
    }

    /**
     * 할인 적용 (새 Booking 반환)
     *
     * @param discount 할인 금액
     * @param couponId 쿠폰 ID
     */
    public Booking withDiscount(Money discount, String couponId) {
        Money newFinalAmount = totalAmount.subtract(discount);
        return new Booking(
                id,
                memberId,
                items,
                status,
                totalAmount,
                discount,
                newFinalAmount,
                couponId,
                createdAt,
                Instant.now()
        );
    }

    /**
     * 예약 항목 추가 (새 Booking 반환)
     *
     * @param item 추가할 항목
     */
    public Booking withAddedItem(BookingItem item) {
        var newItems = new java.util.ArrayList<>(items);
        newItems.add(item);

        Money newTotalAmount = BookingCalculations.calculateTotalAmount(newItems);
        Money newFinalAmount = newTotalAmount.subtract(discountAmount);

        return new Booking(
                id,
                memberId,
                List.copyOf(newItems),
                status,
                newTotalAmount,
                discountAmount,
                newFinalAmount,
                couponId,
                createdAt,
                Instant.now()
        );
    }

    // ============================================
    // [Key Point] 상태 전이 편의 메서드
    // BookingStatus의 전이 메서드 래핑
    // ============================================

    /**
     * 예약 확정 (Pending → Confirmed)
     */
    public Booking confirm(String paymentId) {
        return withStatus(status.confirm(paymentId));
    }

    /**
     * 예약 취소
     */
    public Booking cancel(String reason, Money refundAmount, BookingStatus.Cancelled.CancelledBy cancelledBy) {
        return withStatus(status.cancel(reason, refundAmount, cancelledBy));
    }

    /**
     * 이용 완료
     */
    public Booking complete() {
        return withStatus(status.complete());
    }

    /**
     * 노쇼 처리
     */
    public Booking markNoShow(Money penaltyAmount) {
        return withStatus(status.markNoShow(penaltyAmount));
    }

    // ============================================
    // 조회 헬퍼 메서드
    // ============================================

    /**
     * 예약 항목 수
     */
    public int itemCount() {
        return items.size();
    }

    /**
     * 결제 대기 중인지 확인
     */
    public boolean isPending() {
        return status instanceof BookingStatus.Pending;
    }

    /**
     * 확정된 예약인지 확인
     */
    public boolean isConfirmed() {
        return status instanceof BookingStatus.Confirmed;
    }

    /**
     * 취소된 예약인지 확인
     */
    public boolean isCancelled() {
        return status instanceof BookingStatus.Cancelled;
    }

    /**
     * 할인이 적용되었는지 확인
     */
    public boolean hasDiscount() {
        return !discountAmount.isZero();
    }

    /**
     * 쿠폰이 적용되었는지 확인
     */
    public boolean hasCoupon() {
        return couponId != null && !couponId.isBlank();
    }
}
