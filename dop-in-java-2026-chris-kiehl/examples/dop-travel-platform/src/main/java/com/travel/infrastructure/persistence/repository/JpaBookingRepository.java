package com.travel.infrastructure.persistence.repository;

import com.travel.domain.booking.*;
import com.travel.domain.member.MemberId;
import com.travel.infrastructure.persistence.entity.BookingEntity;
import com.travel.infrastructure.persistence.mapper.BookingMapper;
import com.travel.shared.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA 기반 예약 Repository 구현
 *
 * <h2>핵심 개념 (Key Concept): Ch 6 Imperative Shell</h2>
 * <pre>
 * [IS] Repository는 Imperative Shell:
 * - DB I/O 수행
 * - Entity ↔ Domain 변환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 구현 전략</h2>
 * <pre>
 * [Key Point] Spring Data JPA Repository + Mapper 조합:
 *
 * 1. JpaRepository&lt;BookingEntity, String&gt; - JPA 기능 사용
 * 2. BookingMapper - Entity ↔ Domain 변환
 * 3. BookingRepository 인터페이스 구현 - 도메인 타입으로 노출
 * </pre>
 */
@Repository
public class JpaBookingRepository implements BookingRepository {

    private final BookingJpaRepository jpaRepository;
    private final BookingMapper mapper;

    public JpaBookingRepository(BookingJpaRepository jpaRepository, BookingMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Result<Booking, BookingError> findById(BookingId id) {
        return jpaRepository.findById(id.value().toString())
                .map(entity -> Result.<Booking, BookingError>success(mapper.toDomain(entity)))
                .orElseGet(() -> Result.failure(new BookingError.NotFound(id)));
    }

    @Override
    public Optional<Booking> findByIdOptional(BookingId id) {
        return jpaRepository.findById(id.value().toString())
                .map(mapper::toDomain);
    }

    @Override
    public List<Booking> findByMemberId(MemberId memberId) {
        return jpaRepository.findByMemberId(memberId.value().toString())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Booking> findByMemberIdAndStatus(MemberId memberId, Class<? extends BookingStatus> statusClass) {
        String status = toStatusString(statusClass);
        return jpaRepository.findByMemberIdAndStatus(memberId.value().toString(), status)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Booking> findExpiredPendingBookings() {
        return jpaRepository.findExpiredPendingBookings(Instant.now())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity = mapper.toEntity(booking);
        BookingEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsById(BookingId id) {
        return jpaRepository.existsById(id.value().toString());
    }

    @Override
    public boolean existsByMemberIdAndProductId(MemberId memberId, String productId) {
        return jpaRepository.existsByMemberIdAndProductId(memberId.value().toString(), productId);
    }

    // ============================================
    // 헬퍼 메서드
    // ============================================

    private String toStatusString(Class<? extends BookingStatus> statusClass) {
        if (statusClass == BookingStatus.Pending.class) return "PENDING";
        if (statusClass == BookingStatus.Confirmed.class) return "CONFIRMED";
        if (statusClass == BookingStatus.Cancelled.class) return "CANCELLED";
        if (statusClass == BookingStatus.Completed.class) return "COMPLETED";
        if (statusClass == BookingStatus.NoShow.class) return "NO_SHOW";
        throw new IllegalArgumentException("Unknown status class: " + statusClass);
    }
}

/**
 * Spring Data JPA Repository 인터페이스
 */
interface BookingJpaRepository extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByMemberId(String memberId);

    @Query("SELECT b FROM BookingEntity b WHERE b.memberId = :memberId AND b.status = :status")
    List<BookingEntity> findByMemberIdAndStatus(String memberId, String status);

    @Query("SELECT b FROM BookingEntity b WHERE b.status = 'PENDING' AND b.statusData LIKE '%expiresAt%' " +
           "AND FUNCTION('JSON_EXTRACT', b.statusData, '$.expiresAt') < :now")
    List<BookingEntity> findExpiredPendingBookings(Instant now);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BookingEntity b " +
           "JOIN b.items i WHERE b.memberId = :memberId AND i.productId = :productId")
    boolean existsByMemberIdAndProductId(String memberId, String productId);
}
