package org.demo.footballgateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@AutoConfigureWireMock(port = 0)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "PLAYERS_URI=http://localhost:${wiremock.server.port}",
        "ALBUMS_URI=http://localhost:${wiremock.server.port}",
})
public class RoutesTests {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void testPlayersRoute() {
        // Arrange
        stubFor(get(urlEqualTo("/players"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                                {
                                    "dateOfBirth": "1990-01-01",
                                    "id": "p1",
                                    "name": "Alice",
                                    "number": 5,
                                    "position": "Defender"
                                },
                                {
                                    "dateOfBirth": "1994-02-04",
                                    "id": "p2",
                                    "name": "Bob",
                                    "number": 31,
                                    "position": "Midfielder"
                                }
                            ]
                        """)));

        // Act and Assert
        webClient.get().uri("/api/players").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("p1")
                .jsonPath("$[0].name").isEqualTo("Alice")
                .jsonPath("$[1].id").isEqualTo("p2")
                .jsonPath("$[1].name").isEqualTo("Bob");
    }
}
