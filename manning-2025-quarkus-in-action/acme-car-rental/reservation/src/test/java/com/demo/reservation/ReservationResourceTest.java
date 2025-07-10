package com.demo.reservation;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.demo.reservation.inventory.Car;
import com.demo.reservation.inventory.GraphQLInventoryClient;
import com.demo.reservation.reservation.Reservation;
import com.demo.reservation.rest.ReservationResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class ReservationResourceTest {

    @TestHTTPEndpoint(ReservationResource.class)
    @TestHTTPResource
    URL resrervationsUrl;

    @TestHTTPEndpoint(ReservationResource.class)
    @TestHTTPResource("availability")
    URL availabilityUrl;

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
                .post(resrervationsUrl)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", notNullValue());
    }

    @DisabledOnIntegrationTest(forArtifactTypes = ArtifactType.NATIVE_BINARY)
    @Test
    void testMakingAReservationAndCheckAvailability() {
        // Check available cars
        GraphQLInventoryClient inventoryClient = Mockito.mock(GraphQLInventoryClient.class);
        Car car = new Car(1L, "ABC123", "Toyota", "Camry");
        Mockito.when(inventoryClient.listAllCars())
                .thenReturn(Collections.singletonList(car));
        QuarkusMock.installMockForType(inventoryClient, GraphQLInventoryClient.class);
        String startDate = "2023-10-01";
        String endDate = "2023-10-30";
        Car[] cars = RestAssured.given()
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when().get(availabilityUrl)
                .then().statusCode(HttpStatus.SC_OK)
                .log().body()
                .extract().as(Car[].class);
        Car availableCar = cars[0];

        // Reserve a car
        Reservation reservation = new Reservation(
                null,
                availableCar.id(),
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(reservation)
                .when()
                .post(resrervationsUrl)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("carId", is(availableCar.id().intValue()));

        // Check available cars again
        RestAssured.given()
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when().get(availabilityUrl)
                .then().statusCode(HttpStatus.SC_OK)
                // print body for debugging
                .log().body()
                .body("$", notNullValue())
                .body("size()", is(0)); // No cars available after reservation
    }
}
