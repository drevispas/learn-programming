package com.travel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 결제 JPA Entity
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "payment_method_type", nullable = false)
    private String paymentMethodType;

    @Column(name = "payment_method_data", columnDefinition = "TEXT")
    private String paymentMethodData; // JSON

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "status_data", columnDefinition = "TEXT")
    private String statusData; // JSON

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "idempotency_key_expires_at")
    private Instant idempotencyKeyExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {}

    public static PaymentEntity create() {
        PaymentEntity entity = new PaymentEntity();
        entity.id = UUID.randomUUID().toString();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }

    // Getter / Setter

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public String getPaymentMethodData() { return paymentMethodData; }
    public void setPaymentMethodData(String paymentMethodData) { this.paymentMethodData = paymentMethodData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusData() { return statusData; }
    public void setStatusData(String statusData) { this.statusData = statusData; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getIdempotencyKeyExpiresAt() { return idempotencyKeyExpiresAt; }
    public void setIdempotencyKeyExpiresAt(Instant idempotencyKeyExpiresAt) {
        this.idempotencyKeyExpiresAt = idempotencyKeyExpiresAt;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
