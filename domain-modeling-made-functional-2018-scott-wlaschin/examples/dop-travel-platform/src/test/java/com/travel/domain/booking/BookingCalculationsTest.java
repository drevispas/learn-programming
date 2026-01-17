package com.travel.domain.booking;

import com.travel.shared.types.Currency;
import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BookingCalculations 단위 테스트
 */
@DisplayName("BookingCalculations - 순수 함수 테스트")
class BookingCalculationsTest {

    @Nested
    @DisplayName("calculateTotalAmount")
    class CalculateTotalAmount {

        @Test
        @DisplayName("단일 숙박 항목의 총 금액 계산")
        void single_accommodation_item() {
            // Given
            var item = new BookingItem.Accommodation(
                    "ROOM-001",
                    "호텔 A",
                    "디럭스",
                    new DateRange(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 12)),
                    Money.krw(150000), // 1박 15만원
                    2
            );

            // When
            Money total = BookingCalculations.calculateTotalAmount(List.of(item));

            // Then: 2박 × 15만원 = 30만원
            assertEquals(Money.krw(300000), total);
        }

        @Test
        @DisplayName("여러 항목의 총 금액 합산")
        void multiple_items() {
            // Given
            var accommodation = new BookingItem.Accommodation(
                    "ROOM-001", "호텔", "디럭스",
                    new DateRange(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 12)),
                    Money.krw(100000), 2
            );

            var flight = new BookingItem.Flight(
                    "FL-001", "항공사", "KE123",
                    "ICN", "NRT",
                    LocalDateTime.of(2025, 1, 10, 9, 0),
                    LocalDateTime.of(2025, 1, 10, 11, 30),
                    BookingItem.Flight.SeatClass.ECONOMY,
                    Money.krw(200000), 2
            );

            // When
            Money total = BookingCalculations.calculateTotalAmount(List.of(accommodation, flight));

            // Then: (2박 × 10만원) + (20만원 × 2인) = 20만원 + 40만원 = 60만원
            assertEquals(Money.krw(600000), total);
        }

        @Test
        @DisplayName("빈 목록은 0원 반환")
        void empty_list_returns_zero() {
            // When
            Money total = BookingCalculations.calculateTotalAmount(List.of());

            // Then
            assertEquals(Money.ZERO_KRW, total);
        }
    }

    @Nested
    @DisplayName("calculateRefundAmount")
    class CalculateRefundAmount {

        @Test
        @DisplayName("7일 전 취소는 100% 환불")
        void seven_days_before_full_refund() {
            // Given
            Money paidAmount = Money.krw(100000);

            // When
            Money refund = BookingCalculations.calculateRefundAmount(paidAmount, 7);

            // Then
            assertEquals(Money.krw(100000), refund);
        }

        @Test
        @DisplayName("3일 전 취소는 70% 환불")
        void three_days_before_partial_refund() {
            // Given
            Money paidAmount = Money.krw(100000);

            // When
            Money refund = BookingCalculations.calculateRefundAmount(paidAmount, 3);

            // Then
            assertEquals(Money.krw(70000), refund);
        }

        @Test
        @DisplayName("1일 전 취소는 50% 환불")
        void one_day_before_half_refund() {
            // Given
            Money paidAmount = Money.krw(100000);

            // When
            Money refund = BookingCalculations.calculateRefundAmount(paidAmount, 1);

            // Then
            assertEquals(Money.krw(50000), refund);
        }

        @Test
        @DisplayName("당일 취소는 환불 불가")
        void same_day_no_refund() {
            // Given
            Money paidAmount = Money.krw(100000);

            // When
            Money refund = BookingCalculations.calculateRefundAmount(paidAmount, 0);

            // Then
            assertEquals(Money.krw(0), refund);
        }
    }

    @Nested
    @DisplayName("calculatePercentDiscount")
    class CalculatePercentDiscount {

        @Test
        @DisplayName("10% 할인 계산")
        void ten_percent_discount() {
            // Given
            Money total = Money.krw(100000);

            // When
            Money discount = BookingCalculations.calculatePercentDiscount(total, 10);

            // Then
            assertEquals(Money.krw(10000), discount);
        }

        @Test
        @DisplayName("0% 할인은 0원")
        void zero_percent_no_discount() {
            // Given
            Money total = Money.krw(100000);

            // When
            Money discount = BookingCalculations.calculatePercentDiscount(total, 0);

            // Then
            assertEquals(Money.krw(0), discount);
        }
    }
}
