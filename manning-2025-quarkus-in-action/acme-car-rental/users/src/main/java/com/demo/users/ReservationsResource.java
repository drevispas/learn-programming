package com.demo.users;

import com.demo.users.model.Reservation;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDate;
import java.util.Collection;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;

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
    public TemplateInstance get() {
        Collection<Reservation> reservations = reservationClient.getAllReservations();
        return Templates.listofreservations(reservations);
    }
}
