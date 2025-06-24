package com.demo.reservation.rental;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

@RegisterRestClient(baseUri = "http://localhost:8082")
@Path("/rental")
public interface RentalClient {

    /**
     * Start a rental for the given user and reservation.
     *
     * @param userId        the ID of the user
     * @param reservationId the ID of the reservation
     * @return the started rental
     */
    @POST
    @Path("/start/{userId}/{reservationId}")
    Rental start(@RestPath String userId, @RestPath Long reservationId);
}
