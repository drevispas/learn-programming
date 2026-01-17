package com.travel.application.booking;

import com.travel.domain.booking.BookingItem;
import com.travel.domain.member.MemberId;
import com.travel.shared.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 예약 생성 커맨드 - 입력 데이터 캡슐화
 *
 * <h2>목적 (Purpose)</h2>
 * UseCase에 필요한 입력 데이터를 불변 객체로 캡슐화하고 유효성 검증
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Validation</h2>
 * <pre>
 * [Key Point] 커맨드 생성 시점에 모든 유효성 검증 수행:
 *
 * CreateBookingCommand.validate(memberId, items, couponId)
 *     → Validation&lt;CreateBookingCommand, String&gt;
 *
 * Valid: 커맨드 객체 반환
 * Invalid: 모든 오류 목록 반환 (fail-slow)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 FC/IS 경계</h2>
 * <pre>
 * [Key Point] 커맨드는 Imperative Shell → Functional Core 경계:
 *
 * Controller (IS)
 *     ↓ DTO
 * CreateBookingCommand.validate() (경계)
 *     ↓ Validation&lt;Command, Error&gt;
 * UseCase (IS) → BookingCalculations (FC)
 * </pre>
 *
 * @param memberId 회원 ID
 * @param items    예약 항목들
 * @param couponId 쿠폰 ID (nullable)
 */
public record CreateBookingCommand(
        MemberId memberId,
        List<BookingItem> items,
        String couponId
) {

    public CreateBookingCommand {
        // [Ch 2] 불변 리스트로 방어적 복사
        items = items != null ? List.copyOf(items) : List.of();
    }

    // ============================================
    // [Key Point] Applicative Validation
    // Ch 5: 모든 오류를 수집하는 검증
    // ============================================

    /**
     * 입력 데이터 검증 후 커맨드 생성
     *
     * <pre>
     * [Key Point] Applicative Validation:
     * - 모든 검증을 독립적으로 실행
     * - 실패 시 모든 오류 수집 (fail-slow)
     *
     * 예시:
     * validate(null, [], "COUPON123")
     * → Invalid(["회원 ID는 필수입니다", "예약 항목은 최소 1개 필요합니다"])
     * </pre>
     *
     * @param memberId 회원 ID
     * @param items    예약 항목들
     * @param couponId 쿠폰 ID (nullable)
     * @return 검증 결과
     */
    public static Validation<CreateBookingCommand, String> validate(
            MemberId memberId,
            List<BookingItem> items,
            String couponId
    ) {
        return Validation.combine(
                validateMemberId(memberId),
                validateItems(items),
                (validMemberId, validItems) -> new CreateBookingCommand(validMemberId, validItems, couponId)
        );
    }

    /**
     * 회원 ID 검증
     */
    private static Validation<MemberId, String> validateMemberId(MemberId memberId) {
        if (memberId == null) {
            return Validation.invalid("회원 ID는 필수입니다");
        }
        return Validation.valid(memberId);
    }

    /**
     * 예약 항목 검증
     */
    private static Validation<List<BookingItem>, String> validateItems(List<BookingItem> items) {
        if (items == null || items.isEmpty()) {
            return Validation.invalid("예약 항목은 최소 1개 필요합니다");
        }

        List<String> errors = new ArrayList<>();

        // 최대 항목 수 검증
        if (items.size() > 10) {
            errors.add("예약 항목은 최대 10개까지 가능합니다");
        }

        // 중복 상품 검증
        long uniqueProducts = items.stream()
                .map(BookingItem::productId)
                .distinct()
                .count();
        if (uniqueProducts != items.size()) {
            errors.add("중복된 상품이 포함되어 있습니다");
        }

        if (!errors.isEmpty()) {
            return Validation.invalid(errors);
        }

        return Validation.valid(items);
    }

    // ============================================
    // 헬퍼 메서드
    // ============================================

    /**
     * 쿠폰 적용 여부
     */
    public boolean hasCoupon() {
        return couponId != null && !couponId.isBlank();
    }

    /**
     * 예약 항목 수
     */
    public int itemCount() {
        return items.size();
    }
}
