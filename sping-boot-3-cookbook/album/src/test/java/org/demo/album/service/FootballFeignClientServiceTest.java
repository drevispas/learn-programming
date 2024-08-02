package org.demo.album.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.demo.album.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"football.api.url=http://localhost:7979"})
class FootballFeignClientServiceTest {

    @BeforeAll
    static void init() {
        WireMockServer wireMockServer = new WireMockServer(7979);
        wireMockServer.start();
        WireMock.configureFor(7979);
    }

    @Autowired
    FootballClientService footballClientService;

    @Test
    public void testListPlayers() {
        // Arrange
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/players"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                    {
                                        "id": "p1",
                                        "integer": null,
                                        "name": "Alice",
                                        "position": "Defender",
                                        "dateOfBirth": "1990-01-01"
                                    },
                                    {
                                        "id": "p2",
                                        "integer": null,
                                        "name": "Bob",
                                        "position": "Midfielder",
                                        "dateOfBirth": "1994-02-04"
                                    }
                                ]
                                """)));
        // Act
        List<Player> players = footballClientService.getPlayers();
        // Assert
        assertEquals(2, players.size());
        Player expectePlayer1 = new Player("p1", null, "Alice", "Defender", LocalDate.parse("1990-01-01"));
        Player expectePlayer2 = new Player("p2", null, "Bob", "Midfielder", LocalDate.parse("1994-02-04"));
        assertIterableEquals(List.of(expectePlayer1, expectePlayer2), players);
    }

    @Test
    public void testGetPlayer_exists() {
        // Arrange
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/players/p1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "p1",
                                    "integer": null,
                                    "name": "Alice",
                                    "position": "Defender",
                                    "dateOfBirth": "1990-01-01"
                                }
                                """)));
        // Act
        Optional<Player> player = footballClientService.getPlayer("p1");
        // Assert
        Player expectePlayer = new Player("p1", null, "Alice", "Defender", LocalDate.parse("1990-01-01"));
        assertTrue(player.isPresent());
        assertEquals(expectePlayer, player.get());
    }

    @Test
    public void testGetPlayer_not_exist() {
        // Arrange
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/players/p3"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(404)));
        // Act
        Optional<Player> player = footballClientService.getPlayer("p3");
        // Assert
        assertTrue(player.isEmpty());
    }
}
