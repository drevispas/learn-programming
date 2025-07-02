package com.demo.reservation.reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    List<Reservation> findAll();

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    void deleteById(Long id);
}
