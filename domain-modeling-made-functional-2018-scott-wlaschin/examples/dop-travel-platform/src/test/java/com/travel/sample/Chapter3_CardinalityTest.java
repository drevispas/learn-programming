package com.travel.sample;

import com.travel.domain.booking.BookingItem;
import com.travel.domain.booking.BookingStatus;
import com.travel.domain.product.accommodation.RoomType;
import com.travel.shared.types.DateRange;
import com.travel.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chapter 3: Cardinality (곱/합 타입) 학습 테스트
 *
 * <h2>학습 목표</h2>
 * <ul>
 *   <li>Product Type(곱 타입)과 Sum Type(합 타입) 이해</li>
 *   <li>Cardinality(카디널리티) 개념 이해</li>
 *   <li>sealed interface로 Sum Type 구현</li>
 *   <li>패턴 매칭의 장점 체험</li>
 * </ul>
 */
@DisplayName("[Ch 3] Cardinality - 곱/합 타입")
class Chapter3_CardinalityTest {

    @Nested
    @DisplayName("[Ch 3.1] Cardinality 개념")
    class CardinalityConcept {

        @Test
        @DisplayName("boolean N개의 조합은 2^N 가지 상태를 만든다")
        void boolean_combination_creates_many_states() {
            // [안티패턴] boolean 필드 5개 사용 시
            // boolean isPending, isConfirmed, isPaid, isCancelled, isCompleted
            // → 2^5 = 32가지 상태 가능!
            //
            // 대부분은 무의미하거나 불가능한 상태:
            // - isPending=true && isCompleted=true (???)
            // - isConfirmed=true && isCancelled=true (???)

            int booleanCount = 5;
            int possibleStates = (int) Math.pow(2, booleanCount);
            int validStates = 5; // Pending, Confirmed, Cancelled, Completed, NoShow

            // [Key Point] 32가지 중 5가지만 유효!
            assertTrue(possibleStates > validStates * 3);

            // [해결책] sealed interface로 정확히 5가지만 허용
            // → BookingStatus 참조
        }

        @Test
        @DisplayName("Sum Type은 정확한 가짓수만 허용한다")
        void sum_type_allows_exact_variants() {
            // Given: sealed interface BookingStatus
            // permits Pending, Confirmed, Cancelled, Completed, NoShow

            // Then: 정확히 5가지 상태만 가능
            BookingStatus pending = BookingStatus.Pending.withDefaultExpiry();
            BookingStatus confirmed = BookingStatus.Confirmed.now("PAY-001");
            BookingStatus cancelled = new BookingStatus.Cancelled(
                    "사용자 요청", Instant.now(), null, BookingStatus.Cancelled.CancelledBy.USER
            );
            BookingStatus completed = BookingStatus.Completed.now();
            BookingStatus noShow = new BookingStatus.NoShow(Instant.now(), null);

            // [Key Point] 이 외의 상태는 컴파일러가 허용하지 않음
            assertInstanceOf(BookingStatus.Pending.class, pending);
            assertInstanceOf(BookingStatus.Confirmed.class, confirmed);
            assertInstanceOf(BookingStatus.Cancelled.class, cancelled);
            assertInstanceOf(BookingStatus.Completed.class, completed);
            assertInstanceOf(BookingStatus.NoShow.class, noShow);
        }
    }

    @Nested
    @DisplayName("[Ch 3.2] Sum Type과 패턴 매칭")
    class SumTypeAndPatternMatching {

        @Test
        @DisplayName("패턴 매칭으로 모든 케이스를 안전하게 처리한다")
        void pattern_matching_handles_all_cases() {
            // Given
            BookingStatus status = BookingStatus.Confirmed.now("PAY-001");

            // When: 패턴 매칭 (switch expression)
            String message = switch (status) {
                case BookingStatus.Pending p ->
                        "결제 대기 중 (만료: " + p.expiresAt() + ")";
                case BookingStatus.Confirmed c ->
                        "예약 확정 (결제 ID: " + c.paymentId() + ")";
                case BookingStatus.Cancelled c ->
                        "예약 취소 (사유: " + c.reason() + ")";
                case BookingStatus.Completed c ->
                        "이용 완료 (" + c.completedAt() + ")";
                case BookingStatus.NoShow n ->
                        "노쇼 처리 (" + n.occurredAt() + ")";
                // [Key Point] default 없음! 새 상태 추가 시 컴파일 에러 발생
            };

            // Then
            assertTrue(message.contains("PAY-001"));
        }

        @Test
        @DisplayName("각 variant는 해당 상태에 필요한 데이터만 보유한다")
        void each_variant_has_relevant_data_only() {
            // Pending: 만료 시간
            var pending = BookingStatus.Pending.withDefaultExpiry();
            assertNotNull(pending.expiresAt());
            assertNotNull(pending.createdAt());

            // Confirmed: 결제 ID, 확정 시간
            var confirmed = BookingStatus.Confirmed.now("PAY-001");
            assertEquals("PAY-001", confirmed.paymentId());
            assertNotNull(confirmed.confirmedAt());

            // Cancelled: 취소 사유, 환불 금액, 취소 주체
            var cancelled = new BookingStatus.Cancelled(
                    "변심", Instant.now(), Money.krw(50000), BookingStatus.Cancelled.CancelledBy.USER
            );
            assertEquals("변심", cancelled.reason());
            assertTrue(cancelled.hasRefund());
            assertEquals(BookingStatus.Cancelled.CancelledBy.USER, cancelled.cancelledBy());

            // [Key Point] Pending 상태에서 "결제 ID"에 접근? → 컴파일 에러!
            // confirmed.expiresAt(); → 컴파일 에러! (Confirmed에 없는 필드)
        }
    }

    @Nested
    @DisplayName("[Ch 3.3] 다형성 Sum Type")
    class PolymorphicSumType {

        @Test
        @DisplayName("BookingItem은 다양한 상품 유형을 하나의 타입으로 표현한다")
        void booking_item_represents_different_product_types() {
            // Accommodation
            BookingItem accommodation = new BookingItem.Accommodation(
                    "ROOM-001", "호텔", "디럭스",
                    new DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
                    Money.krw(150000), 2
            );

            // Flight
            BookingItem flight = new BookingItem.Flight(
                    "FL-001", "항공사", "KE123",
                    "서울", "도쿄",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2),
                    BookingItem.Flight.SeatClass.ECONOMY,
                    Money.krw(200000), 1
            );

            // 공통 인터페이스 사용
            assertEquals("숙박", accommodation.type());
            assertEquals("항공", flight.type());

            // 패턴 매칭으로 유형별 처리
            int guestCount = switch (accommodation) {
                case BookingItem.Accommodation a -> a.guestCount();
                case BookingItem.Flight f -> f.passengerCount();
                case BookingItem.TravelPackage p -> p.participantCount();
            };
            assertEquals(2, guestCount);
        }
    }

    @Nested
    @DisplayName("[Ch 3.4] sealed interface와 확장성")
    class SealedInterfaceExtensibility {

        @Test
        @DisplayName("RoomType은 다양한 객실 유형을 표현한다")
        void room_type_represents_various_room_types() {
            // Standard (데이터 없음)
            RoomType standard = new RoomType.Standard();

            // Deluxe (데이터 없음)
            RoomType deluxe = new RoomType.Deluxe();

            // Family (추가 데이터: maxChildren)
            RoomType family = new RoomType.Family(3);

            // Penthouse (추가 데이터: floorNumber)
            RoomType penthouse = new RoomType.Penthouse(30);

            // 패턴 매칭
            String description = switch (penthouse) {
                case RoomType.Standard s -> "일반 객실";
                case RoomType.Deluxe d -> "디럭스 객실";
                case RoomType.Suite s -> "스위트룸";
                case RoomType.Family f -> "패밀리룸 (어린이 " + f.maxChildren() + "명)";
                case RoomType.Penthouse p -> "펜트하우스 " + p.floorNumber() + "층";
            };

            assertEquals("펜트하우스 30층", description);

            // 각 타입의 가격 배수
            assertEquals(1.0, standard.priceMultiplier());
            assertEquals(1.3, deluxe.priceMultiplier());
            assertEquals(1.5, family.priceMultiplier());
            assertEquals(5.0, penthouse.priceMultiplier());
        }
    }
}
