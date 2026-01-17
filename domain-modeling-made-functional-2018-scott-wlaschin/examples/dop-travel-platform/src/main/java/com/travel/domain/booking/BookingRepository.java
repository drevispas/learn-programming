package com.travel.domain.booking;

import com.travel.domain.member.MemberId;
import com.travel.shared.Result;

import java.util.List;
import java.util.Optional;

/**
 * 예약 Repository 인터페이스
 *
 * <h2>목적 (Purpose)</h2>
 * 예약 영속성 추상화 - 도메인 레이어가 인프라에 의존하지 않도록 함
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Functional Core / Imperative Shell</h2>
 * <pre>
 * [IS] Repository는 Imperative Shell의 일부:
 * - I/O 작업 (DB 읽기/쓰기)
 * - 부수효과 발생
 *
 * [Key Point] 인터페이스를 도메인에, 구현을 인프라에 배치:
 *   domain/booking/BookingRepository.java        (인터페이스)
 *   infrastructure/repository/JpaBookingRepository.java (구현)
 *
 * → 도메인이 인프라에 의존하지 않음 (의존성 역전 원칙)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Result 반환</h2>
 * <pre>
 * [Key Point] 조회 실패를 Result로 명시적 표현:
 *   Result&lt;Booking, BookingError&gt; findById(BookingId id);
 *
 * vs Optional 사용:
 *   Optional&lt;Booking&gt; findById(BookingId id);
 *   → 실패 원인을 표현할 수 없음
 * </pre>
 */
public interface BookingRepository {

    // ============================================
    // 단건 조회
    // ============================================

    /**
     * ID로 예약 조회
     *
     * @param id 예약 ID
     * @return 예약 (Result로 감싸서 반환)
     */
    Result<Booking, BookingError> findById(BookingId id);

    /**
     * ID로 예약 조회 (Optional 버전)
     *
     * <p>단순 존재 여부 확인용</p>
     */
    Optional<Booking> findByIdOptional(BookingId id);

    // ============================================
    // 목록 조회
    // ============================================

    /**
     * 회원의 모든 예약 조회
     *
     * @param memberId 회원 ID
     * @return 예약 목록
     */
    List<Booking> findByMemberId(MemberId memberId);

    /**
     * 회원의 특정 상태 예약 조회
     *
     * @param memberId 회원 ID
     * @param statusClass 상태 클래스 (예: BookingStatus.Pending.class)
     * @return 예약 목록
     */
    List<Booking> findByMemberIdAndStatus(MemberId memberId, Class<? extends BookingStatus> statusClass);

    /**
     * 결제 대기 중이며 만료된 예약 조회
     *
     * @return 만료된 Pending 예약 목록
     */
    List<Booking> findExpiredPendingBookings();

    // ============================================
    // 저장/수정
    // ============================================

    /**
     * 예약 저장 (생성 또는 수정)
     *
     * @param booking 저장할 예약
     * @return 저장된 예약
     */
    Booking save(Booking booking);

    // ============================================
    // 존재 여부 확인
    // ============================================

    /**
     * 예약 존재 여부 확인
     *
     * @param id 예약 ID
     * @return 존재하면 true
     */
    boolean existsById(BookingId id);

    /**
     * 회원이 특정 상품을 이미 예약했는지 확인
     *
     * @param memberId 회원 ID
     * @param productId 상품 ID
     * @return 이미 예약했으면 true
     */
    boolean existsByMemberIdAndProductId(MemberId memberId, String productId);
}
