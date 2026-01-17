package com.travel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 회원 JPA Entity
 */
@Entity
@Table(name = "members")
public class MemberEntity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "membership_tier", nullable = false)
    private String membershipTier;

    @Column(name = "total_booking_amount")
    private java.math.BigDecimal totalBookingAmount;

    @Column(name = "booking_count")
    private int bookingCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MemberEntity() {}

    public static MemberEntity create() {
        MemberEntity entity = new MemberEntity();
        entity.id = UUID.randomUUID().toString();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        entity.membershipTier = "BRONZE";
        entity.emailVerified = false;
        entity.bookingCount = 0;
        return entity;
    }

    // Getter / Setter

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMembershipTier() { return membershipTier; }
    public void setMembershipTier(String membershipTier) { this.membershipTier = membershipTier; }

    public java.math.BigDecimal getTotalBookingAmount() { return totalBookingAmount; }
    public void setTotalBookingAmount(java.math.BigDecimal totalBookingAmount) {
        this.totalBookingAmount = totalBookingAmount;
    }

    public int getBookingCount() { return bookingCount; }
    public void setBookingCount(int bookingCount) { this.bookingCount = bookingCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
