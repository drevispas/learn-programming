package com.demo.users;

import com.demo.users.model.Car;
import com.demo.users.model.Reservation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.time.LocalDate;
import java.util.Collection;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

@RegisterRestClient(baseUri = "http://localhost:8081")
@Path("reservations")
public interface ReservationClient {

    @GET
    @Path("all")
    Collection<Reservation> getAllReservations();

    @POST
    Reservation make(Reservation reservation);

    @GET
    @Path("availability")
    Collection<Car> availability(@RestQuery LocalDate startDate, @RestQuery LocalDate endDate);
}
