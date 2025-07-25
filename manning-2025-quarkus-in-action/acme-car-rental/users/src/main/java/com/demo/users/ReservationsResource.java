package com.demo.users;

import com.demo.users.model.Car;
import com.demo.users.model.Reservation;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
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
import java.util.Objects;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/")
public class ReservationsResource {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance index(
                LocalDate startDate,
                LocalDate endDate,
                String name
        );

        public static native TemplateInstance listofreservations(
                Collection<Reservation> reservations
        );

        public static native TemplateInstance availablecars(
                Collection<Car> cars,
                LocalDate startDate,
                LocalDate endDate
        );
    }

    @Inject
    SecurityContext securityContext;

    @RestClient
    ReservationClient reservationClient;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@RestQuery LocalDate startDate, @RestQuery LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().plusDays(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().plusDays(7);
        }
        String name = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "unknown";
        return Templates.index(startDate, endDate, name);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/get")
    public TemplateInstance getReservations() {
        Collection<Reservation> reservations = reservationClient.getAllReservations();
        return Templates.listofreservations(reservations);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/available")
    public TemplateInstance getAvailableCars(@RestQuery LocalDate startDate, @RestQuery LocalDate endDate) {
        Collection<Car> cars = reservationClient.availability(startDate, endDate);
        return Templates.availablecars(cars, startDate, endDate);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/reserve")
    public RestResponse<TemplateInstance> create(
            @RestForm LocalDate startDate,
            @RestForm LocalDate endDate,
            @RestForm Long carId) {
        Reservation reservation = new Reservation(null, null, carId, startDate, endDate);
        reservationClient.make(reservation);
        return RestResponse.ResponseBuilder
                .ok(getReservations())
                .header("HX-Trigger-After-Swap", "update-available-cars-list")
                .build();
    }

    @DELETE
    @Produces(MediaType.TEXT_HTML)
    @Path("/cancel")
    public RestResponse<TemplateInstance> cancel(@RestQuery("reservationId") Long reservationId) {
        reservationClient.cancel(reservationId);
        return RestResponse.ResponseBuilder
                .ok(getReservations())
                .header("HX-Trigger-After-Swap", "update-available-cars-list")
                .build();
    }
}
