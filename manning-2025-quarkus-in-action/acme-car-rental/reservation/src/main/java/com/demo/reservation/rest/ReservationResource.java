package com.demo.reservation.rest;

import com.demo.reservation.inventory.Car;
import com.demo.reservation.inventory.GraphQLInventoryClient;
import com.demo.reservation.inventory.InventoryClient;
import com.demo.reservation.rental.Rental;
import com.demo.reservation.rental.RentalClient;
import com.demo.reservation.reservation.Reservation;
import com.demo.reservation.reservation.ReservationRepository;
import io.quarkus.logging.Log;
import io.smallrye.graphql.client.GraphQLClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;

@Slf4j
@Path("reservations")
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private final ReservationRepository reservationRepository;
    private final InventoryClient inventoryClient;
    private final RentalClient rentalClient;
    @Inject
    SecurityContext securityContext;

    public ReservationResource(
            ReservationRepository reservationRepository,
            @GraphQLClient("inventory") GraphQLInventoryClient inventoryClient,
            @RestClient RentalClient rentalClient) {
        this.reservationRepository = reservationRepository;
        this.inventoryClient = inventoryClient;
        this.rentalClient = rentalClient;
    }

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
        Log.info("Making reservation: " + reservation);
        Log.infof("Reservation start date: %s", reservation.startDate());
        // save the reservation
        Reservation result = reservationRepository.save(reservation);
        String userId = "x";
        if (reservation.startDate().equals(LocalDate.now())) {
            Rental rental = rentalClient.start(userId, result.id());
            Log.info("Successfully started rental: " + rental);
        }
        return result;
    }

    @GET
    public List<Reservation> list() {
        // list all reservations
        return reservationRepository.findAll();
    }

    @GET
    @Path("all")
    public List<Reservation> allReservations() {
        String userId = securityContext.getUserPrincipal().getName() != null ? securityContext.getUserPrincipal().getName() : null;
        return reservationRepository.findAll().stream()
                .filter(reservation -> userId == null || Objects.equals(reservation.userId(), userId))
                .toList();
    }
}
