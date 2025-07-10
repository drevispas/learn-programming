package com.demo.reservation;

import com.demo.reservation.reservation.Reservation;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReservationRepositoryTest {

    @Test
    @Transactional
    void testCreateReservation() {
        Reservation reservation = new Reservation(
                null,
                123L,
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 5));
        reservation.persist();
        Reservation savedReservation = Reservation.findById(reservation.id);
        Assertions.assertEquals(1, Reservation.count());
        Assertions.assertNotNull(savedReservation);
        Assertions.assertEquals(reservation.carId, savedReservation.carId);
    }
}
