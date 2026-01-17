package com.travel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 예약 JPA Entity - 영속성 계층 모델
 *
 * <h2>목적 (Purpose)</h2>
 * 도메인 Record와 분리된 JPA Entity로 영속성 관심사 분리
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 JPA Entity와 도메인 Record 공존</h2>
 * <pre>
 * [Key Point] 도메인 모델과 영속성 모델 분리:
 *
 * 도메인 레이어:
 *   Booking (record) - 불변, 순수 도메인 로직
 *   BookingStatus (sealed interface) - Sum Type
 *
 * 인프라 레이어:
 *   BookingEntity (JPA Entity) - 가변, JPA 관심사
 *   status: String (enum 변환)
 *
 * BookingMapper로 상호 변환
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 왜 분리하는가?</h2>
 * <pre>
 * [Why 분리]
 * 1. JPA Entity는 가변(mutable)해야 함 - setter, @Id 등
 * 2. 도메인 Record는 불변(immutable) 권장
 * 3. sealed interface는 JPA에서 직접 매핑 어려움
 * 4. 도메인 로직이 JPA 어노테이션에 오염되지 않음
 *
 * [Trade-off]
 * - 장점: 관심사 분리, 테스트 용이성, 유연성
 * - 단점: 변환 코드 필요, 동기화 주의
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] Entity를 도메인 로직에서 직접 사용 → 반드시 Mapper 통해 변환</li>
 *   <li>[Trap] 도메인 Record에 JPA 어노테이션 추가 → 관심사 오염</li>
 *   <li>[Trap] sealed interface를 그대로 DB 저장 → enum/String 변환 필요</li>
 * </ul>
 */
@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    /**
     * [Ch 9] sealed interface → String 변환
     *
     * <p>BookingStatus.Pending, Confirmed 등을 문자열로 저장</p>
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * [Ch 9] 상태별 추가 데이터를 JSON으로 저장
     *
     * <p>예: Confirmed의 paymentId, Cancelled의 reason 등</p>
     */
    @Column(name = "status_data", columnDefinition = "TEXT")
    private String statusData;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "coupon_id")
    private String couponId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * [Ch 9] 예약 항목은 별도 테이블에 저장
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItemEntity> items = new ArrayList<>();

    // ============================================
    // JPA 요구사항: 기본 생성자
    // ============================================

    public BookingEntity() {}

    // ============================================
    // 빌더 패턴 (가변 Entity용)
    // ============================================

    public static BookingEntity create() {
        BookingEntity entity = new BookingEntity();
        entity.id = UUID.randomUUID().toString();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }

    // ============================================
    // Getter / Setter
    // [Key Point] Entity는 가변이므로 setter 제공
    // ============================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusData() {
        return statusData;
    }

    public void setStatusData(String statusData) {
        this.statusData = statusData;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BookingItemEntity> getItems() {
        return items;
    }

    public void setItems(List<BookingItemEntity> items) {
        this.items = items;
    }

    public void addItem(BookingItemEntity item) {
        items.add(item);
        item.setBooking(this);
    }

    public void removeItem(BookingItemEntity item) {
        items.remove(item);
        item.setBooking(null);
    }

    // ============================================
    // JPA 콜백
    // ============================================

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
