package com.demo.reservation.reservation;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Reservation(
        Long id,
        Long carId,
        LocalDate startDate,
        LocalDate endDate
) {

    public boolean isReserved(LocalDate startDate, LocalDate endDate) {
        log.info("Checking if reservation {} is reserved between {} and {}", this, startDate, endDate);
        return (!(this.startDate.isAfter(endDate) || this.endDate.isBefore(startDate)));
    }

    public Reservation withId(Long id) {
        return new Reservation(id, this.carId, this.startDate, this.endDate);
    }
}
