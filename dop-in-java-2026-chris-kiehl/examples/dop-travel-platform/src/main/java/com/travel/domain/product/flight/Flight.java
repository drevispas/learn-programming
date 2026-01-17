package com.travel.domain.product.flight;

import com.travel.shared.types.Money;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 항공편 - Value Object
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * [Key Point] 항공편 정보를 불변 레코드로 표현
 * </pre>
 *
 * @param id              항공편 ID
 * @param airline         항공사
 * @param flightNumber    편명
 * @param departure       출발지
 * @param arrival         도착지
 * @param departureTime   출발 시간
 * @param arrivalTime     도착 시간
 * @param seatPrices      좌석 등급별 가격
 * @param availableSeats  좌석 등급별 잔여 좌석
 */
public record Flight(
        String id,
        String airline,
        String flightNumber,
        String departure,
        String arrival,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Map<SeatClass, Money> seatPrices,
        Map<SeatClass, Integer> availableSeats
) {

    public Flight {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("항공편 ID는 필수입니다");
        if (airline == null || airline.isBlank()) throw new IllegalArgumentException("항공사는 필수입니다");
        if (flightNumber == null || flightNumber.isBlank()) throw new IllegalArgumentException("편명은 필수입니다");
        if (departure == null || departure.isBlank()) throw new IllegalArgumentException("출발지는 필수입니다");
        if (arrival == null || arrival.isBlank()) throw new IllegalArgumentException("도착지는 필수입니다");
        if (departureTime == null) throw new IllegalArgumentException("출발 시간은 필수입니다");
        if (arrivalTime == null) throw new IllegalArgumentException("도착 시간은 필수입니다");
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("도착 시간은 출발 시간 이후여야 합니다");
        }
        if (seatPrices == null) seatPrices = Map.of();
        else seatPrices = Map.copyOf(seatPrices);
        if (availableSeats == null) availableSeats = Map.of();
        else availableSeats = Map.copyOf(availableSeats);
    }

    /**
     * 비행 시간
     */
    public Duration flightDuration() {
        return Duration.between(departureTime, arrivalTime);
    }

    /**
     * 비행 시간 (분)
     */
    public long flightDurationMinutes() {
        return flightDuration().toMinutes();
    }

    /**
     * 특정 좌석 등급의 가격 조회
     */
    public Money getPrice(SeatClass seatClass) {
        return seatPrices.get(seatClass);
    }

    /**
     * 특정 좌석 등급의 잔여 좌석 조회
     */
    public int getAvailableSeats(SeatClass seatClass) {
        return availableSeats.getOrDefault(seatClass, 0);
    }

    /**
     * 예약 가능 여부
     */
    public boolean isAvailable(SeatClass seatClass, int passengers) {
        return getAvailableSeats(seatClass) >= passengers;
    }

    /**
     * 노선 표시 문자열
     */
    public String route() {
        return departure + " → " + arrival;
    }
}
