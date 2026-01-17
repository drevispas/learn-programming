package com.travel.domain.product.travelpackage;

import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;

import java.util.List;

/**
 * 여행 패키지 - Value Object
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * [Key Point] 여행 패키지 정보를 불변 레코드로 표현
 * </pre>
 *
 * @param id            패키지 ID
 * @param name          패키지 이름
 * @param description   설명
 * @param destination   목적지
 * @param dateRange     여행 기간
 * @param pricePerPerson 1인 가격
 * @param minParticipants 최소 인원
 * @param maxParticipants 최대 인원
 * @param includes      포함 항목
 * @param excludes      불포함 항목
 * @param itinerary     일정
 */
public record TravelPackage(
        String id,
        String name,
        String description,
        String destination,
        DateRange dateRange,
        Money pricePerPerson,
        int minParticipants,
        int maxParticipants,
        List<String> includes,
        List<String> excludes,
        List<DayItinerary> itinerary
) {

    public TravelPackage {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("패키지 ID는 필수입니다");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("패키지 이름은 필수입니다");
        if (destination == null || destination.isBlank()) throw new IllegalArgumentException("목적지는 필수입니다");
        if (dateRange == null) throw new IllegalArgumentException("여행 기간은 필수입니다");
        if (pricePerPerson == null) throw new IllegalArgumentException("1인 가격은 필수입니다");
        if (minParticipants <= 0) throw new IllegalArgumentException("최소 인원은 1명 이상이어야 합니다");
        if (maxParticipants < minParticipants) {
            throw new IllegalArgumentException("최대 인원은 최소 인원 이상이어야 합니다");
        }
        if (includes == null) includes = List.of();
        else includes = List.copyOf(includes);
        if (excludes == null) excludes = List.of();
        else excludes = List.copyOf(excludes);
        if (itinerary == null) itinerary = List.of();
        else itinerary = List.copyOf(itinerary);
    }

    /**
     * 일정
     */
    public record DayItinerary(
            int day,
            String title,
            String description,
            List<String> activities
    ) {
        public DayItinerary {
            if (day <= 0) throw new IllegalArgumentException("일차는 1 이상이어야 합니다");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 필수입니다");
            if (activities == null) activities = List.of();
            else activities = List.copyOf(activities);
        }
    }

    /**
     * 여행 일수
     */
    public long days() {
        return dateRange.days();
    }

    /**
     * 총 금액 계산
     *
     * @param participants 참가자 수
     * @return 총 금액
     */
    public Money calculateTotalPrice(int participants) {
        validateParticipants(participants);
        return pricePerPerson.multiply(participants);
    }

    /**
     * 참가자 수 검증
     */
    public void validateParticipants(int participants) {
        if (participants < minParticipants) {
            throw new IllegalArgumentException(
                    "최소 인원은 " + minParticipants + "명입니다. 요청: " + participants);
        }
        if (participants > maxParticipants) {
            throw new IllegalArgumentException(
                    "최대 인원은 " + maxParticipants + "명입니다. 요청: " + participants);
        }
    }
}
