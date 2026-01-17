package com.travel.domain.product.accommodation;

import com.travel.shared.types.Money;

/**
 * 객실 - Value Object
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * [Key Point] 객실 정보를 불변 레코드로 표현
 * - 호텔 ID, 객실 유형, 요금 등 포함
 * </pre>
 *
 * @param id           객실 ID
 * @param hotelId      호텔 ID
 * @param hotelName    호텔 이름
 * @param roomType     객실 유형
 * @param nightlyRate  1박 요금
 * @param maxGuests    최대 수용 인원
 * @param amenities    편의시설
 * @param description  설명
 */
public record Room(
        String id,
        String hotelId,
        String hotelName,
        RoomType roomType,
        Money nightlyRate,
        int maxGuests,
        java.util.List<String> amenities,
        String description
) {

    public Room {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("객실 ID는 필수입니다");
        if (hotelId == null || hotelId.isBlank()) throw new IllegalArgumentException("호텔 ID는 필수입니다");
        if (hotelName == null || hotelName.isBlank()) throw new IllegalArgumentException("호텔 이름은 필수입니다");
        if (roomType == null) throw new IllegalArgumentException("객실 유형은 필수입니다");
        if (nightlyRate == null) throw new IllegalArgumentException("1박 요금은 필수입니다");
        if (maxGuests <= 0) throw new IllegalArgumentException("최대 수용 인원은 1명 이상이어야 합니다");
        if (amenities == null) amenities = java.util.List.of();
        else amenities = java.util.List.copyOf(amenities);
    }

    /**
     * 숙박료 계산
     *
     * @param nights 숙박 일수
     * @return 총 숙박료
     */
    public Money calculatePrice(int nights) {
        return nightlyRate.multiply(nights);
    }
}
