package com.travel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예약 항목 JPA Entity
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 Sum Type → 단일 테이블 전략</h2>
 * <pre>
 * [Key Point] BookingItem sealed interface를 단일 테이블로 저장:
 *
 * 도메인: BookingItem.Accommodation | Flight | TravelPackage
 * DB: 하나의 테이블에 item_type 구분자 + 모든 필드
 *
 * [Why 단일 테이블]
 * - 조인 없이 빠른 조회
 * - NULL 허용 필드 발생 (trade-off)
 * </pre>
 */
@Entity
@Table(name = "booking_items")
public class BookingItemEntity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    /**
     * 항목 유형: ACCOMMODATION, FLIGHT, PACKAGE
     */
    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    // ============================================
    // 숙박 전용 필드
    // ============================================

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "nightly_rate")
    private BigDecimal nightlyRate;

    @Column(name = "guest_count")
    private Integer guestCount;

    // ============================================
    // 항공 전용 필드
    // ============================================

    @Column(name = "airline")
    private String airline;

    @Column(name = "flight_number")
    private String flightNumber;

    @Column(name = "departure")
    private String departure;

    @Column(name = "arrival")
    private String arrival;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "seat_class")
    private String seatClass;

    @Column(name = "passenger_count")
    private Integer passengerCount;

    // ============================================
    // 패키지 전용 필드
    // ============================================

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "includes", columnDefinition = "TEXT")
    private String includes; // JSON array

    @Column(name = "participant_count")
    private Integer participantCount;

    // ============================================
    // 생성자
    // ============================================

    protected BookingItemEntity() {}

    public static BookingItemEntity create() {
        BookingItemEntity entity = new BookingItemEntity();
        entity.id = UUID.randomUUID().toString();
        return entity;
    }

    // ============================================
    // Getter / Setter
    // ============================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public BookingEntity getBooking() { return booking; }
    public void setBooking(BookingEntity booking) { this.booking = booking; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    // Accommodation fields
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }

    // Flight fields
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getDeparture() { return departure; }
    public void setDeparture(String departure) { this.departure = departure; }

    public String getArrival() { return arrival; }
    public void setArrival(String arrival) { this.arrival = arrival; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }

    public Integer getPassengerCount() { return passengerCount; }
    public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }

    // Package fields
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getIncludes() { return includes; }
    public void setIncludes(String includes) { this.includes = includes; }

    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
}
