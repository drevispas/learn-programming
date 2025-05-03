package com.demo.reservation.rest;

import com.demo.reservation.inventory.Car;
import com.demo.reservation.inventory.InventoryClient;
import com.demo.reservation.reservation.Reservation;
import com.demo.reservation.reservation.ReservationRepository;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestQuery;

@Slf4j
@RequiredArgsConstructor
@Path("reservations")
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private final ReservationRepository reservationRepository;
    private final InventoryClient inventoryClient;

    @GET
    @Path("availability")
    public Collection<Car> availability(
            // todo: validate startDate <= endDate
            @RestQuery LocalDate startDate,
            @RestQuery LocalDate endDate) {
        // list all cars startDate the inventory
        List<Car> availableCars = inventoryClient.listAllCars();
        // list all reservations
        List<Reservation> reservations = reservationRepository.findAll();
        // available cars: not in the reservations + reserved but not in the period
        Map<Long, Car> availableCarsMap = new HashMap<>();
        for (Car car : availableCars) {
            availableCarsMap.put(car.id(), car);
        }
        for (Reservation reservation : reservations) {
            log.info("Reservation: {}", reservation);
            if (reservation.isReserved(startDate, endDate)) {
                availableCarsMap.remove(reservation.carId());
            }
        }
        return availableCarsMap.values();
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Reservation make(Reservation reservation) {
        // save the reservation
        return reservationRepository.save(reservation);
    }

    @GET
    public List<Reservation> list() {
        // list all reservations
        return reservationRepository.findAll();
    }
}
