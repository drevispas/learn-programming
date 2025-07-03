package com.demo.reservation.reservation;

import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class InMemoryReservationRepository implements ReservationRepository {

    private final AtomicLong counter = new AtomicLong();
    private final List<Reservation> reservations = new CopyOnWriteArrayList<>();

    @Override
    public List<Reservation> findAll() {
        return Collections.unmodifiableList(reservations);
    }

    @Override
    public Reservation save(Reservation reservation) {
        Reservation savedReservation = reservation.withId(counter.incrementAndGet());
        reservations.add(savedReservation);
        return savedReservation;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservations.stream()
                .filter(reservation -> reservation.id().equals(id))
                .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        reservations.removeIf(reservation -> reservation.id().equals(id));
    }
}
