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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * List all available cars for the given period.
     *
     * @param startDate start date of the period
     * @param endDate   end date of the period
     * @return list of available cars
     */
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

    /**
     * Make a reservation for the given car and period.
     *
     * @param reservation reservation to make
     * @return made reservation
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Reservation make(Reservation reservation) {
        Log.info("Making reservation: " + reservation);
        Log.infof("Reservation start date: %s", reservation.startDate());
        // save the reservation
        Reservation authorizedReservation = reservation.withUserId(getUserId());
        Reservation result = reservationRepository.save(authorizedReservation);
        if (authorizedReservation.startDate().equals(LocalDate.now())) {
            Rental rental = rentalClient.start(authorizedReservation.userId(), result.id());
            Log.info("Successfully started rental: " + rental);
        }
        return result;
    }

    /**
     * List all reservations.
     *
     * @return list of reservations
     */
    @GET
    public List<Reservation> list() {
        // list all reservations
        return reservationRepository.findAll();
    }

    /**
     * List all reservations for the current user.
     *
     * @return list of reservations
     */
    @GET
    @Path("all")
    public List<Reservation> allReservations() {
        String userId = getUserId();
        return reservationRepository.findAll().stream()
                .filter(reservation -> userId == null || Objects.equals(reservation.userId(), userId))
                .toList();
    }

    @DELETE
    @Path("{id}")
    public void cancel(@PathParam("id") Long id) {
        Log.infof("Cancelling reservation with id %d", id);
        // find the reservation
        Optional<Reservation> reservation = reservationRepository.findById(id);
        if (reservation.isEmpty()) {
            Log.warnf("Reservation with id %d not found", id);
            return;
        }
        // delete the reservation
        reservationRepository.deleteById(reservation.get().id());
        Log.infof("Cancelled reservation with id %d", id);
    }

    private String getUserId() {
        return securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : null;
    }
}
