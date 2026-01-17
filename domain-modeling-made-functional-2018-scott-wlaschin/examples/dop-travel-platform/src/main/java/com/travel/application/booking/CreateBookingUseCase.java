package com.travel.application.booking;

import com.travel.domain.booking.*;
import com.travel.domain.coupon.Coupon;
import com.travel.domain.coupon.CouponRepository;
import com.travel.domain.member.Member;
import com.travel.domain.member.MemberRepository;
import com.travel.shared.Result;
import com.travel.shared.types.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 생성 UseCase - Imperative Shell (샌드위치 아키텍처)
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 생성 워크플로우를 조율하며 부수효과(I/O)를 관리
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core / Imperative Shell</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────┐
 * │  Top Bun (IS): 데이터 수집                          │
 * │  - Repository에서 엔티티 조회                       │
 * │  - 외부 서비스 호출                                 │
 * ├─────────────────────────────────────────────────────┤
 * │  Meat (FC): 순수 비즈니스 로직                      │
 * │  - BookingDomainService.xxx()                      │
 * │  - BookingCalculations.xxx()                       │
 * │  - 모든 입력이 준비된 상태에서 순수 계산            │
 * ├─────────────────────────────────────────────────────┤
 * │  Bottom Bun (IS): 부수효과 실행                     │
 * │  - Repository.save()                               │
 * │  - 이벤트 발행                                      │
 * │  - 외부 시스템 알림                                 │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Railway-Oriented Programming</h2>
 * <pre>
 * [Key Point] Result 체이닝으로 에러 전파:
 *
 * validateCommand()
 *     .flatMap(this::loadMember)
 *     .flatMap(this::loadCoupon)
 *     .flatMap(this::createBooking)
 *     .flatMap(this::applyDiscount)
 *     .map(this::saveBooking)
 *
 * 중간에 실패하면 이후 단계는 자동으로 건너뜀
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] UseCase에 비즈니스 로직 작성 → FC로 분리해야 함</li>
 *   <li>[Trap] FC에서 Repository 호출 → IS에서만 해야 함</li>
 *   <li>[Why @Transactional] 여러 Repository 작업을 원자적으로</li>
 * </ul>
 */
@Service
public class CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;

    // [Key Point] 생성자 주입 - 테스트 시 Mock 주입 용이
    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            MemberRepository memberRepository,
            CouponRepository couponRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.memberRepository = memberRepository;
        this.couponRepository = couponRepository;
    }

    // ============================================
    // [Key Point] 메인 실행 메서드 - 샌드위치 구조
    // ============================================

    /**
     * 예약 생성 실행
     *
     * <pre>
     * [Ch 6] 샌드위치 아키텍처:
     *
     * === Top Bun (IS) ===
     * 1. 커맨드 검증
     * 2. 회원 조회
     * 3. 쿠폰 조회 (있으면)
     * 4. 재고 확인
     *
     * === Meat (FC) ===
     * 5. 예약 생성 (순수 함수)
     * 6. 할인 적용 (순수 함수)
     *
     * === Bottom Bun (IS) ===
     * 7. 예약 저장
     * 8. 이벤트 발행 (TODO)
     * </pre>
     *
     * @param command 예약 생성 커맨드
     * @return 생성된 예약 또는 오류
     */
    @Transactional
    public Result<Booking, BookingError> execute(CreateBookingCommand command) {

        // ========================================
        // [Top Bun] 데이터 수집 (Imperative Shell)
        // ========================================

        // 1. 회원 조회
        Result<Member, BookingError> memberResult = loadMember(command.memberId());
        if (memberResult.isFailure()) {
            return Result.failure(memberResult.errorOrNull());
        }
        Member member = memberResult.getOrThrow();

        // 2. 쿠폰 조회 (있으면)
        Money discountAmount = Money.ZERO_KRW;
        if (command.hasCoupon()) {
            Result<Coupon, BookingError> couponResult = loadAndValidateCoupon(
                    command.couponId(),
                    command.memberId()
            );
            if (couponResult.isFailure()) {
                return Result.failure(couponResult.errorOrNull());
            }
            discountAmount = couponResult.getOrThrow().discountAmount();
        }

        // 3. 재고 확인 (각 항목별)
        Result<Void, BookingError> stockResult = checkAvailability(command.items());
        if (stockResult.isFailure()) {
            return Result.failure(stockResult.errorOrNull());
        }

        // ========================================
        // [Meat] 순수 비즈니스 로직 (Functional Core)
        // ========================================

        // 4. [FC] 예약 생성 (순수 함수)
        Booking booking = Booking.create(command.memberId(), command.items());

        // 5. [FC] 할인 적용 (순수 함수)
        if (!discountAmount.isZero()) {
            Result<Booking, BookingError> discountResult =
                    BookingDomainService.applyDiscount(booking, discountAmount, command.couponId());
            if (discountResult.isFailure()) {
                return discountResult;
            }
            booking = discountResult.getOrThrow();
        }

        // ========================================
        // [Bottom Bun] 부수효과 실행 (Imperative Shell)
        // ========================================

        // 6. [IS] 예약 저장
        Booking savedBooking = bookingRepository.save(booking);

        // 7. [IS] 쿠폰 사용 처리
        if (command.hasCoupon()) {
            markCouponAsUsed(command.couponId());
        }

        // 8. [IS] 이벤트 발행 (TODO: 이벤트 시스템 구현 시)
        // eventPublisher.publish(new BookingCreatedEvent(savedBooking));

        return Result.success(savedBooking);
    }

    // ============================================
    // [Top Bun] 데이터 조회 메서드 (Imperative Shell)
    // ============================================

    /**
     * 회원 조회 (I/O)
     */
    private Result<Member, BookingError> loadMember(
            com.travel.domain.member.MemberId memberId
    ) {
        return memberRepository.findById(memberId)
                .map(member -> Result.<Member, BookingError>success(member))
                .orElse(Result.failure(new BookingError.ValidationFailed(
                        java.util.List.of("회원을 찾을 수 없습니다: " + memberId)
                )));
    }

    /**
     * 쿠폰 조회 및 검증 (I/O)
     */
    private Result<Coupon, BookingError> loadAndValidateCoupon(
            String couponId,
            com.travel.domain.member.MemberId memberId
    ) {
        return couponRepository.findById(couponId)
                .map(coupon -> {
                    // 쿠폰 유효성 검증
                    if (coupon.isExpired()) {
                        return Result.<Coupon, BookingError>failure(
                                new BookingError.CouponNotApplicable(couponId, "만료된 쿠폰입니다")
                        );
                    }
                    if (coupon.isUsed()) {
                        return Result.<Coupon, BookingError>failure(
                                new BookingError.CouponNotApplicable(couponId, "이미 사용된 쿠폰입니다")
                        );
                    }
                    if (!coupon.isOwnedBy(memberId)) {
                        return Result.<Coupon, BookingError>failure(
                                new BookingError.CouponNotApplicable(couponId, "소유자가 아닙니다")
                        );
                    }
                    return Result.<Coupon, BookingError>success(coupon);
                })
                .orElse(Result.failure(
                        new BookingError.CouponNotApplicable(couponId, "쿠폰을 찾을 수 없습니다")
                ));
    }

    /**
     * 재고/가용성 확인 (I/O)
     *
     * <p>실제로는 각 상품 유형별 Repository/Service 호출 필요</p>
     */
    private Result<Void, BookingError> checkAvailability(
            java.util.List<BookingItem> items
    ) {
        // TODO: 실제 재고 확인 로직 구현
        // 현재는 항상 가용으로 처리
        for (BookingItem item : items) {
            // switch (item) {
            //     case BookingItem.Accommodation a ->
            //         accommodationService.checkAvailability(a.roomId(), a.dateRange());
            //     case BookingItem.Flight f ->
            //         flightService.checkAvailability(f.flightId(), f.passengerCount());
            //     case BookingItem.TravelPackage p ->
            //         packageService.checkAvailability(p.packageId(), p.participantCount());
            // }
        }
        return Result.success(null);
    }

    // ============================================
    // [Bottom Bun] 부수효과 메서드 (Imperative Shell)
    // ============================================

    /**
     * 쿠폰 사용 처리 (I/O)
     */
    private void markCouponAsUsed(String couponId) {
        couponRepository.findById(couponId)
                .ifPresent(coupon -> {
                    Coupon usedCoupon = coupon.markAsUsed();
                    couponRepository.save(usedCoupon);
                });
    }
}
