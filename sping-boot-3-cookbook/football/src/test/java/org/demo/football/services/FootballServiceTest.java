package org.demo.football.services;

import org.demo.football.exceptions.AlreadyExistsException;
import org.demo.football.exceptions.NotFoundException;
import org.demo.football.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FootballService.class)
class FootballServiceTest {

//    @TestConfiguration
//    static class FootballServiceTestContextConfiguration {
//        @Bean
//        public FootballService footballService() {
//            return new FootballService();
//        }
//    }

    @Autowired
    FootballService footballService;

    @Test
    public void testListPlayers() {
        // Arrange

        // Act
        List<Player> players = footballService.listPlayers();

        // Assert
        assertFalse(players.isEmpty());
    }

    @Test
    public void testGetPlayer_exists() {
        // Arrange
        String ID = "p1";

        // Act
        Player player = footballService.getPlayer(ID);

        // Assert
        assertNotNull(player);
    }

    @Test
    public void testGetPlayer_not_exist() {
        // Arrange
        String ID = "p3";

        // Act and Assert
        assertThrows(NotFoundException.class, () -> footballService.getPlayer(ID));
    }

    @Test
    public void testAddPlayer_not_exist() {
        // Arrange
        Player newPlayer = new Player("p3", 7, "Charlie", "Forward", LocalDate.parse("1995-03-07"));

        // Act
        Player addedPlayer = footballService.addPlayer(newPlayer);

        // Assert
        assertNotNull(addedPlayer);
        assertEquals(newPlayer, addedPlayer);

        // Clean up
        footballService.deletePlayer(newPlayer.id());
    }

    @Test
    public void testAddPlayer_already_exists() {
        // Arrange
        Player player = new Player("p1", 7, "Charlie", "Forward", LocalDate.parse("1995-03-07"));

        // Act and Assert
        assertThrows(AlreadyExistsException.class, () -> footballService.addPlayer(player));
    }

    @Test
    public void testUpdatePlayer_exists() {
        // Arrange
        String ID = "p1";
        Player originalPlayer = footballService.getPlayer(ID);
        Player player = new Player(ID, 7, "Charlie", "Forward", LocalDate.parse("1995-03-07"));

        // Act
        Player updatedPlayer = footballService.updatePlayer(ID, player);

        // Assert
        assertNotNull(updatedPlayer);
        assertEquals(player, updatedPlayer);

        // Clean
        footballService.updatePlayer(ID, originalPlayer);
    }

    @Test
    public void testUpdatePlayer_not_exist() {
        // Arrange
        String ID = "p3";
        Player player = new Player(ID, 7, "Charlie", "Forward", LocalDate.parse("1995-03-07"));

        // Act and Assert
        assertThrows(NotFoundException.class, () -> footballService.updatePlayer(ID, player));
    }

    @Test
    public void testDeletePlayer_exists() {
        // Arrange
        String ID = "p1";
        Player originalPlayer = footballService.getPlayer(ID);

        // Act
        footballService.deletePlayer(ID);

        // Assert
        assertThrows(NotFoundException.class, () -> footballService.getPlayer(ID));

        // Clean
        footballService.addPlayer(originalPlayer);
    }

    @Test
    public void testDeletePlayer_not_exist() {
        // Arrange
        String ID = "p3";

        // Act and Assert
        assertThrows(NotFoundException.class, () -> footballService.deletePlayer(ID));
    }
}
