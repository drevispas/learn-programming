package com.demo.reservation;

import com.demo.reservation.reservation.Reservation;
import com.demo.reservation.reservation.ReservationRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReservationRepositoryTest {

    @Inject
    ReservationRepository reservationRepository;

    @Test
    void testCreateReservation() {
        Reservation reservation = new Reservation(
                null,
                123L,
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 5));
        Reservation savedReservation = reservationRepository.save(reservation);
        Assertions.assertNotNull(savedReservation.id());
        Assertions.assertTrue(reservationRepository.findAll().contains(savedReservation));
    }
}
