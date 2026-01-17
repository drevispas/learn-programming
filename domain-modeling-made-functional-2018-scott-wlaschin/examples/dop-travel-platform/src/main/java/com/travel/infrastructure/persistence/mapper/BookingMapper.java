package com.travel.infrastructure.persistence.mapper;

import com.travel.domain.booking.*;
import com.travel.domain.member.MemberId;
import com.travel.infrastructure.persistence.entity.BookingEntity;
import com.travel.infrastructure.persistence.entity.BookingItemEntity;
import com.travel.shared.types.Currency;
import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 예약 Mapper - Domain Record ↔ JPA Entity 변환
 *
 * <h2>목적 (Purpose)</h2>
 * 도메인 모델(Record/sealed interface)과 영속성 모델(JPA Entity) 간 변환
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 Mapper 패턴</h2>
 * <pre>
 * [Key Point] 변환 책임을 Mapper에 집중:
 *
 * Booking (도메인) ←→ BookingEntity (JPA)
 *   - toDomain(entity): Entity → Record
 *   - toEntity(domain): Record → Entity
 *
 * [Why Mapper]
 * 1. 변환 로직 집중 → 유지보수 용이
 * 2. 도메인과 인프라 의존성 분리
 * 3. sealed interface ↔ String/enum 변환 처리
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 Sum Type 변환</h2>
 * <pre>
 * BookingStatus (sealed interface)
 *   ↓ toEntity
 * status: "PENDING", statusData: "{\"expiresAt\":...}"
 *   ↓ toDomain
 * BookingStatus.Pending(createdAt, expiresAt)
 *
 * [Key Point] 각 variant의 추가 데이터는 JSON으로 직렬화
 * </pre>
 */
@Component
public class BookingMapper {

    // ============================================
    // [Key Point] Domain → Entity 변환
    // ============================================

    /**
     * Booking → BookingEntity 변환
     */
    public BookingEntity toEntity(Booking booking) {
        BookingEntity entity = new BookingEntity();

        entity.setId(booking.id().value().toString());
        entity.setMemberId(booking.memberId().value().toString());
        entity.setTotalAmount(booking.totalAmount().amount());
        entity.setDiscountAmount(booking.discountAmount().amount());
        entity.setFinalAmount(booking.finalAmount().amount());
        entity.setCurrency(booking.totalAmount().currency().code());
        entity.setCouponId(booking.couponId());
        entity.setCreatedAt(booking.createdAt());
        entity.setUpdatedAt(booking.updatedAt());

        // [Ch 9] sealed interface → String 변환
        entity.setStatus(toStatusString(booking.status()));
        entity.setStatusData(toStatusData(booking.status()));

        // 항목 변환
        for (BookingItem item : booking.items()) {
            BookingItemEntity itemEntity = toItemEntity(item);
            entity.addItem(itemEntity);
        }

        return entity;
    }

    /**
     * BookingItem → BookingItemEntity 변환
     */
    public BookingItemEntity toItemEntity(BookingItem item) {
        BookingItemEntity entity = BookingItemEntity.create();

        entity.setProductId(item.productId());
        entity.setName(item.name());
        entity.setBasePrice(item.basePrice().amount());
        entity.setCurrency(item.basePrice().currency().code());

        return switch (item) {
            case BookingItem.Accommodation a -> {
                entity.setItemType("ACCOMMODATION");
                entity.setHotelName(a.hotelName());
                entity.setRoomType(a.roomType());
                entity.setCheckInDate(a.dateRange().startDate());
                entity.setCheckOutDate(a.dateRange().endDate());
                entity.setNightlyRate(a.nightlyRate().amount());
                entity.setGuestCount(a.guestCount());
                yield entity;
            }
            case BookingItem.Flight f -> {
                entity.setItemType("FLIGHT");
                entity.setAirline(f.airline());
                entity.setFlightNumber(f.flightNumber());
                entity.setDeparture(f.departure());
                entity.setArrival(f.arrival());
                entity.setDepartureTime(f.departureTime());
                entity.setArrivalTime(f.arrivalTime());
                entity.setSeatClass(f.seatClass().name());
                entity.setPassengerCount(f.passengerCount());
                yield entity;
            }
            case BookingItem.TravelPackage p -> {
                entity.setItemType("PACKAGE");
                entity.setPackageName(p.packageName());
                entity.setDescription(p.description());
                entity.setStartDate(p.dateRange().startDate());
                entity.setEndDate(p.dateRange().endDate());
                entity.setIncludes(String.join(",", p.includes()));
                entity.setParticipantCount(p.participantCount());
                yield entity;
            }
        };
    }

    // ============================================
    // [Key Point] Entity → Domain 변환
    // ============================================

    /**
     * BookingEntity → Booking 변환
     */
    public Booking toDomain(BookingEntity entity) {
        Currency currency = Currency.valueOf(entity.getCurrency());

        List<BookingItem> items = entity.getItems().stream()
                .map(this::toItemDomain)
                .toList();

        return new Booking(
                new BookingId(UUID.fromString(entity.getId())),
                new MemberId(UUID.fromString(entity.getMemberId())),
                items,
                toStatusDomain(entity.getStatus(), entity.getStatusData(), currency),
                new Money(entity.getTotalAmount(), currency),
                new Money(entity.getDiscountAmount(), currency),
                new Money(entity.getFinalAmount(), currency),
                entity.getCouponId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * BookingItemEntity → BookingItem 변환
     */
    public BookingItem toItemDomain(BookingItemEntity entity) {
        Currency currency = Currency.valueOf(entity.getCurrency());

        return switch (entity.getItemType()) {
            case "ACCOMMODATION" -> new BookingItem.Accommodation(
                    entity.getProductId(),
                    entity.getHotelName(),
                    entity.getRoomType(),
                    new DateRange(entity.getCheckInDate(), entity.getCheckOutDate()),
                    new Money(entity.getNightlyRate(), currency),
                    entity.getGuestCount()
            );
            case "FLIGHT" -> new BookingItem.Flight(
                    entity.getProductId(),
                    entity.getAirline(),
                    entity.getFlightNumber(),
                    entity.getDeparture(),
                    entity.getArrival(),
                    entity.getDepartureTime(),
                    entity.getArrivalTime(),
                    BookingItem.Flight.SeatClass.valueOf(entity.getSeatClass()),
                    new Money(entity.getBasePrice(), currency),
                    entity.getPassengerCount()
            );
            case "PACKAGE" -> new BookingItem.TravelPackage(
                    entity.getProductId(),
                    entity.getPackageName(),
                    entity.getDescription(),
                    new DateRange(entity.getStartDate(), entity.getEndDate()),
                    new Money(entity.getBasePrice(), currency),
                    entity.getIncludes() != null ?
                            Arrays.asList(entity.getIncludes().split(",")) : List.of(),
                    entity.getParticipantCount()
            );
            default -> throw new IllegalArgumentException("Unknown item type: " + entity.getItemType());
        };
    }

    // ============================================
    // [Key Point] BookingStatus ↔ String 변환
    // Ch 9: sealed interface를 DB에 저장하기 위한 변환
    // ============================================

    /**
     * BookingStatus → 상태 문자열
     */
    private String toStatusString(BookingStatus status) {
        return switch (status) {
            case BookingStatus.Pending p -> "PENDING";
            case BookingStatus.Confirmed c -> "CONFIRMED";
            case BookingStatus.Cancelled c -> "CANCELLED";
            case BookingStatus.Completed c -> "COMPLETED";
            case BookingStatus.NoShow n -> "NO_SHOW";
        };
    }

    /**
     * BookingStatus → 상태 데이터 JSON
     *
     * <p>[Key Point] 각 상태별 추가 데이터를 JSON으로 저장</p>
     */
    private String toStatusData(BookingStatus status) {
        // 실제로는 JSON 라이브러리 사용 권장
        return switch (status) {
            case BookingStatus.Pending p ->
                    "{\"createdAt\":\"" + p.createdAt() + "\",\"expiresAt\":\"" + p.expiresAt() + "\"}";
            case BookingStatus.Confirmed c ->
                    "{\"paymentId\":\"" + c.paymentId() + "\",\"confirmedAt\":\"" + c.confirmedAt() + "\"}";
            case BookingStatus.Cancelled c ->
                    "{\"reason\":\"" + c.reason() + "\",\"cancelledAt\":\"" + c.cancelledAt() +
                    "\",\"cancelledBy\":\"" + c.cancelledBy() + "\"" +
                    (c.refundAmount() != null ? ",\"refundAmount\":" + c.refundAmount().amount() : "") + "}";
            case BookingStatus.Completed c ->
                    "{\"completedAt\":\"" + c.completedAt() + "\"}";
            case BookingStatus.NoShow n ->
                    "{\"occurredAt\":\"" + n.occurredAt() + "\"" +
                    (n.penaltyAmount() != null ? ",\"penaltyAmount\":" + n.penaltyAmount().amount() : "") + "}";
        };
    }

    /**
     * 상태 문자열 + 데이터 → BookingStatus
     *
     * <p>[Key Point] JSON 파싱하여 적절한 variant 생성</p>
     */
    private BookingStatus toStatusDomain(String status, String statusData, Currency currency) {
        // 실제로는 JSON 파싱 라이브러리 사용 권장
        // 여기서는 단순화된 파싱
        return switch (status) {
            case "PENDING" -> {
                Instant createdAt = extractInstant(statusData, "createdAt");
                Instant expiresAt = extractInstant(statusData, "expiresAt");
                yield new BookingStatus.Pending(createdAt, expiresAt);
            }
            case "CONFIRMED" -> {
                String paymentId = extractString(statusData, "paymentId");
                Instant confirmedAt = extractInstant(statusData, "confirmedAt");
                yield new BookingStatus.Confirmed(paymentId, confirmedAt);
            }
            case "CANCELLED" -> {
                String reason = extractString(statusData, "reason");
                Instant cancelledAt = extractInstant(statusData, "cancelledAt");
                String cancelledByStr = extractString(statusData, "cancelledBy");
                BookingStatus.Cancelled.CancelledBy cancelledBy =
                        BookingStatus.Cancelled.CancelledBy.valueOf(cancelledByStr);
                // refundAmount는 nullable
                Money refundAmount = null;
                if (statusData.contains("refundAmount")) {
                    java.math.BigDecimal amount = extractDecimal(statusData, "refundAmount");
                    refundAmount = new Money(amount, currency);
                }
                yield new BookingStatus.Cancelled(reason, cancelledAt, refundAmount, cancelledBy);
            }
            case "COMPLETED" -> {
                Instant completedAt = extractInstant(statusData, "completedAt");
                yield new BookingStatus.Completed(completedAt);
            }
            case "NO_SHOW" -> {
                Instant occurredAt = extractInstant(statusData, "occurredAt");
                Money penaltyAmount = null;
                if (statusData.contains("penaltyAmount")) {
                    java.math.BigDecimal amount = extractDecimal(statusData, "penaltyAmount");
                    penaltyAmount = new Money(amount, currency);
                }
                yield new BookingStatus.NoShow(occurredAt, penaltyAmount);
            }
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    // ============================================
    // JSON 파싱 헬퍼 (실제로는 Jackson 등 사용)
    // ============================================

    private String extractString(String json, String key) {
        int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private Instant extractInstant(String json, String key) {
        String value = extractString(json, key);
        return Instant.parse(value);
    }

    private java.math.BigDecimal extractDecimal(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        return new java.math.BigDecimal(value);
    }
}
