package com.travel.sample;

import com.travel.domain.booking.Booking;
import com.travel.domain.booking.BookingCalculations;
import com.travel.domain.booking.BookingItem;
import com.travel.domain.booking.BookingStatus;
import com.travel.domain.member.MemberId;
import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chapter 1: DOP 4대 원칙 학습 테스트
 *
 * <h2>학습 목표</h2>
 * <ul>
 *   <li>데이터와 동작의 분리 이해</li>
 *   <li>God Class의 문제점 인식</li>
 *   <li>불변 데이터의 이점 체험</li>
 *   <li>순수 함수의 테스트 용이성 확인</li>
 * </ul>
 */
@DisplayName("[Ch 1] DOP 4대 원칙")
class Chapter1_DOPPrinciplesTest {

    @Nested
    @DisplayName("[Ch 1.1] 데이터와 동작의 분리")
    class DataAndBehaviorSeparation {

        @Test
        @DisplayName("Booking(데이터)과 BookingCalculations(동작)이 분리되어 있다")
        void booking_is_pure_data_without_logic() {
            // Given: Booking record는 순수 데이터
            var item = createAccommodation();

            // When: Booking 생성 (데이터만 담김)
            var booking = Booking.create(MemberId.generate(), List.of(item));

            // Then: Booking은 데이터만 보유, 비즈니스 로직 없음
            assertNotNull(booking.id());
            assertNotNull(booking.totalAmount());
            assertEquals(1, booking.items().size());

            // [Key Point] 계산 로직은 별도 클래스에서 수행
            // BookingCalculations.calculateTotalAmount()
        }

        @Test
        @DisplayName("BookingCalculations는 순수 함수로 구성된다")
        void bookingCalculations_are_pure_functions() {
            // Given: 순수 데이터
            BookingItem item1 = createAccommodation();
            BookingItem item2 = createFlight();
            List<BookingItem> items = List.of(item1, item2);

            // When: 순수 함수 호출 (외부 의존성 없음)
            Money total = BookingCalculations.calculateTotalAmount(items);

            // Then: 동일 입력 → 동일 출력
            Money total2 = BookingCalculations.calculateTotalAmount(items);
            assertEquals(total, total2);

            // [Key Point] 순수 함수는 Mock 없이 테스트 가능!
        }
    }

    @Nested
    @DisplayName("[Ch 1.2] 불변성의 이점")
    class ImmutabilityBenefits {

        @Test
        @DisplayName("Booking 수정 시 원본은 변경되지 않는다")
        void booking_modification_creates_new_instance() {
            // Given: 원본 예약
            var item = createAccommodation();
            var original = Booking.create(MemberId.generate(), List.of(item));
            BookingStatus originalStatus = original.status();

            // When: 상태 변경 (새 객체 반환)
            var confirmed = original.confirm("PAY-123");

            // Then: 원본은 그대로, 새 객체 생성됨
            assertNotSame(original, confirmed);
            assertEquals(originalStatus, original.status()); // 원본 상태 유지
            assertInstanceOf(BookingStatus.Confirmed.class, confirmed.status()); // 새 객체만 변경
        }

        @Test
        @DisplayName("불변 객체는 스레드 안전하다")
        void immutable_objects_are_thread_safe() throws InterruptedException {
            // Given: 불변 Money 객체
            var money = Money.krw(10000);

            // When: 여러 스레드에서 동시 접근
            var results = new java.util.concurrent.CopyOnWriteArrayList<Money>();
            var threads = java.util.stream.IntStream.range(0, 10)
                    .mapToObj(i -> new Thread(() -> {
                        // 읽기 작업 (안전)
                        results.add(money.multiplyPercent(10));
                    }))
                    .toList();

            threads.forEach(Thread::start);
            for (var t : threads) t.join();

            // Then: 모든 스레드가 동일한 결과를 얻음
            assertTrue(results.stream().allMatch(r -> r.equals(Money.krw(1000))));

            // [Key Point] 불변 객체는 동기화 없이 공유 가능
        }
    }

    @Nested
    @DisplayName("[Ch 1.3] 순수 함수의 테스트 용이성")
    class PureFunctionTestability {

        @Test
        @DisplayName("순수 함수는 Mock 없이 단위 테스트 가능하다")
        void pure_functions_need_no_mocks() {
            // Given: 순수 입력 데이터
            List<BookingItem> items = List.of(createAccommodation(), createFlight());

            // When: 순수 함수 호출
            Money total = BookingCalculations.calculateTotalAmount(items);

            // Then: 입력과 출력만 검증
            assertEquals(Money.krw(400000), total);

            // [Key Point] Repository Mock, Service Mock 불필요!
            // 순수 함수는 입력 → 출력 관계만 테스트하면 됨
        }

        @Test
        @DisplayName("환불 금액 계산은 결정적(deterministic)이다")
        void refund_calculation_is_deterministic() {
            // Given
            var paidAmount = Money.krw(100000);

            // When: 동일 조건으로 여러 번 호출
            Money refund1 = BookingCalculations.calculateRefundAmount(paidAmount, 7);
            Money refund2 = BookingCalculations.calculateRefundAmount(paidAmount, 7);
            Money refund3 = BookingCalculations.calculateRefundAmount(paidAmount, 7);

            // Then: 항상 동일한 결과
            assertEquals(refund1, refund2);
            assertEquals(refund2, refund3);
            assertEquals(Money.krw(100000), refund1); // 7일 전: 100% 환불
        }
    }

    // ============================================
    // 테스트 헬퍼
    // ============================================

    private BookingItem.Accommodation createAccommodation() {
        return new BookingItem.Accommodation(
                "ROOM-001",
                "그랜드 호텔",
                "디럭스",
                new DateRange(LocalDate.now().plusDays(7), LocalDate.now().plusDays(9)),
                Money.krw(150000),
                2
        );
    }

    private BookingItem.Flight createFlight() {
        return new BookingItem.Flight(
                "FL-001",
                "대한항공",
                "KE123",
                "ICN",
                "NRT",
                java.time.LocalDateTime.now().plusDays(7),
                java.time.LocalDateTime.now().plusDays(7).plusHours(2),
                BookingItem.Flight.SeatClass.ECONOMY,
                Money.krw(100000),
                1
        );
    }
}
