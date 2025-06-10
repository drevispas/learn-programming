package com.demo.reservation;

import static org.hamcrest.Matchers.notNullValue;

import com.demo.reservation.reservation.Reservation;
import com.demo.reservation.rest.ReservationResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.net.URL;
import java.time.LocalDate;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReservationResourceTest {

    @TestHTTPEndpoint(ReservationResource.class)
    @TestHTTPResource
    URL resrervationResourceUrl;

    @Test
    void testReservationIds() {
        Reservation reservation = new Reservation(
                null,
                123L,
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 5));
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(reservation)
                .when()
                .post(resrervationResourceUrl)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", notNullValue());
    }
}
