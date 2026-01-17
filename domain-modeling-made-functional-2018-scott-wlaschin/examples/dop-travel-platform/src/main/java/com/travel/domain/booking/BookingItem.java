package com.travel.domain.booking;

import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;

import java.time.LocalDateTime;

/**
 * 예약 항목 - 다형성 Sum Type
 *
 * <h2>목적 (Purpose)</h2>
 * 숙박, 항공, 패키지 등 다양한 예약 가능 상품을 하나의 타입으로 표현
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 Sum Type 다형성</h2>
 * <pre>
 * [Key Point] 각 variant가 서로 다른 구조 보유:
 *
 * Accommodation: roomId, dateRange (숙박 기간)
 * Flight: flightId, departureTime, arrivalTime
 * Package: packageId, 포함 항목들
 *
 * [Before] 공통 인터페이스 + 타입 필드:
 *   BookingItem { type: "HOTEL" | "FLIGHT", ... }
 *   → 타입에 따라 무의미한 필드가 null
 *
 * [After] sealed interface:
 *   → 각 variant에 필요한 필드만 존재
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 패턴 매칭</h2>
 * <pre>
 * Money price = switch (item) {
 *     case Accommodation a -> calculateNightlyRate(a);
 *     case Flight f -> f.price();
 *     case TravelPackage p -> calculatePackagePrice(p);
 * };
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] instanceof 체인 사용 → 패턴 매칭이 더 안전하고 명확</li>
 *   <li>[Trap] 공통 필드만 추출 → 다형성 손실</li>
 *   <li>[Why sealed] 새 variant 추가 시 모든 switch문 검토 강제</li>
 * </ul>
 */
public sealed interface BookingItem permits
        BookingItem.Accommodation,
        BookingItem.Flight,
        BookingItem.TravelPackage {

    // ============================================
    // [Key Point] 공통 인터페이스 - 모든 variant가 구현해야 하는 메서드
    // ============================================

    /**
     * 상품 이름
     */
    String name();

    /**
     * 기본 가격 (할인 전)
     */
    Money basePrice();

    /**
     * 상품 ID
     */
    String productId();

    // ============================================
    // [Key Point] 각 variant - 해당 상품 유형에 필요한 데이터만 보유
    // ============================================

    /**
     * 숙박 예약 항목
     *
     * @param roomId       객실 ID
     * @param hotelName    호텔 이름
     * @param roomType     객실 타입 (예: 디럭스, 스위트)
     * @param dateRange    숙박 기간 (체크인 ~ 체크아웃)
     * @param nightlyRate  1박 요금
     * @param guestCount   투숙객 수
     */
    record Accommodation(
            String roomId,
            String hotelName,
            String roomType,
            DateRange dateRange,
            Money nightlyRate,
            int guestCount
    ) implements BookingItem {

        public Accommodation {
            // [Ch 2] Compact Constructor 검증
            if (roomId == null || roomId.isBlank()) {
                throw new IllegalArgumentException("객실 ID는 필수입니다");
            }
            if (hotelName == null || hotelName.isBlank()) {
                throw new IllegalArgumentException("호텔 이름은 필수입니다");
            }
            if (roomType == null || roomType.isBlank()) {
                throw new IllegalArgumentException("객실 타입은 필수입니다");
            }
            if (dateRange == null) {
                throw new IllegalArgumentException("숙박 기간은 필수입니다");
            }
            if (nightlyRate == null) {
                throw new IllegalArgumentException("1박 요금은 필수입니다");
            }
            if (guestCount <= 0) {
                throw new IllegalArgumentException("투숙객 수는 1명 이상이어야 합니다");
            }
        }

        @Override
        public String name() {
            return hotelName + " - " + roomType;
        }

        /**
         * 총 숙박료 계산 (1박 요금 × 숙박 일수)
         */
        @Override
        public Money basePrice() {
            int nights = (int) dateRange.nights();
            return nightlyRate.multiply(Math.max(nights, 1));
        }

        @Override
        public String productId() {
            return roomId;
        }

        /**
         * 숙박 일수 (박)
         */
        public long nights() {
            return dateRange.nights();
        }
    }

    /**
     * 항공권 예약 항목
     *
     * @param flightId      항공편 ID
     * @param airline       항공사
     * @param flightNumber  편명 (예: KE123)
     * @param departure     출발지
     * @param arrival       도착지
     * @param departureTime 출발 시간
     * @param arrivalTime   도착 시간
     * @param seatClass     좌석 등급
     * @param price         가격
     * @param passengerCount 승객 수
     */
    record Flight(
            String flightId,
            String airline,
            String flightNumber,
            String departure,
            String arrival,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            SeatClass seatClass,
            Money price,
            int passengerCount
    ) implements BookingItem {

        /**
         * 좌석 등급
         */
        public enum SeatClass {
            ECONOMY("일반석"),
            PREMIUM_ECONOMY("프리미엄 이코노미"),
            BUSINESS("비즈니스"),
            FIRST("일등석");

            private final String displayName;

            SeatClass(String displayName) {
                this.displayName = displayName;
            }

            public String displayName() {
                return displayName;
            }
        }

        public Flight {
            if (flightId == null || flightId.isBlank()) {
                throw new IllegalArgumentException("항공편 ID는 필수입니다");
            }
            if (airline == null || airline.isBlank()) {
                throw new IllegalArgumentException("항공사는 필수입니다");
            }
            if (flightNumber == null || flightNumber.isBlank()) {
                throw new IllegalArgumentException("편명은 필수입니다");
            }
            if (departure == null || departure.isBlank()) {
                throw new IllegalArgumentException("출발지는 필수입니다");
            }
            if (arrival == null || arrival.isBlank()) {
                throw new IllegalArgumentException("도착지는 필수입니다");
            }
            if (departureTime == null) {
                throw new IllegalArgumentException("출발 시간은 필수입니다");
            }
            if (arrivalTime == null) {
                throw new IllegalArgumentException("도착 시간은 필수입니다");
            }
            if (!arrivalTime.isAfter(departureTime)) {
                throw new IllegalArgumentException("도착 시간은 출발 시간 이후여야 합니다");
            }
            if (seatClass == null) {
                throw new IllegalArgumentException("좌석 등급은 필수입니다");
            }
            if (price == null) {
                throw new IllegalArgumentException("가격은 필수입니다");
            }
            if (passengerCount <= 0) {
                throw new IllegalArgumentException("승객 수는 1명 이상이어야 합니다");
            }
        }

        @Override
        public String name() {
            return airline + " " + flightNumber + " (" + departure + " → " + arrival + ")";
        }

        @Override
        public Money basePrice() {
            return price.multiply(passengerCount);
        }

        @Override
        public String productId() {
            return flightId;
        }

        /**
         * 비행 시간 (분)
         */
        public long flightDurationMinutes() {
            return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
        }
    }

    /**
     * 여행 패키지 예약 항목
     *
     * @param packageId   패키지 ID
     * @param packageName 패키지 이름
     * @param description 설명
     * @param dateRange   여행 기간
     * @param price       패키지 가격
     * @param includes    포함 항목 (숙박, 항공, 투어 등)
     * @param participantCount 참가자 수
     */
    record TravelPackage(
            String packageId,
            String packageName,
            String description,
            DateRange dateRange,
            Money price,
            java.util.List<String> includes,
            int participantCount
    ) implements BookingItem {

        public TravelPackage {
            if (packageId == null || packageId.isBlank()) {
                throw new IllegalArgumentException("패키지 ID는 필수입니다");
            }
            if (packageName == null || packageName.isBlank()) {
                throw new IllegalArgumentException("패키지 이름은 필수입니다");
            }
            if (dateRange == null) {
                throw new IllegalArgumentException("여행 기간은 필수입니다");
            }
            if (price == null) {
                throw new IllegalArgumentException("가격은 필수입니다");
            }
            if (includes == null) {
                includes = java.util.List.of();
            } else {
                includes = java.util.List.copyOf(includes); // 불변 복사
            }
            if (participantCount <= 0) {
                throw new IllegalArgumentException("참가자 수는 1명 이상이어야 합니다");
            }
        }

        @Override
        public String name() {
            return packageName;
        }

        @Override
        public Money basePrice() {
            return price.multiply(participantCount);
        }

        @Override
        public String productId() {
            return packageId;
        }

        /**
         * 여행 일수
         */
        public long days() {
            return dateRange.days();
        }
    }

    // ============================================
    // [Key Point] 기본 메서드 - 패턴 매칭 활용
    // ============================================

    /**
     * 예약 항목 타입 반환
     */
    default String type() {
        return switch (this) {
            case Accommodation a -> "숙박";
            case Flight f -> "항공";
            case TravelPackage p -> "패키지";
        };
    }

    /**
     * 간략 설명
     */
    default String summary() {
        return switch (this) {
            case Accommodation a ->
                    a.hotelName() + " " + a.roomType() + " (" + a.dateRange().nights() + "박)";
            case Flight f ->
                    f.flightNumber() + " " + f.departure() + "→" + f.arrival();
            case TravelPackage p ->
                    p.packageName() + " (" + p.dateRange().days() + "일)";
        };
    }
}
