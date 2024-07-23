package org.demo.football.controller;

import org.demo.football.exceptions.AlreadyExistsException;
import org.demo.football.exceptions.NotFoundException;
import org.demo.football.model.Player;
import org.demo.football.services.FootballService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.demo.football.PlayerTestHelper.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest only loads the web layer, not the full application context.
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mvc;

    // @MockBean replaces the previous bean with a mock in the application context.
    @MockBean
    private FootballService footballService;

    @Test
    public void testListPlayers() throws Exception {
        // Arrange
        given(footballService.listPlayers()).willReturn(PLAYERS);

        // Act
        // MockMvc.perform() returns a ResultActions object that provides methods like andExpect().
        MvcResult result = mvc.perform(get("/players").accept(MediaType.APPLICATION_JSON))
                // HTTP status code between 200 and 299
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andReturn();

        // Assert
        assertArrayEquals(PLAYERS.toArray(), mvcResultToPlayers(result).toArray());
    }

    @Test
    public void testReadPlayer_exist() throws Exception {
        // Arrange
        Player player = PLAYERS.getFirst();
        String ID = player.id();
        given(footballService.getPlayer(ID)).willReturn(player);

        // Act and Assert
        MvcResult result = mvc.perform(get("/players/" + ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(ID))
                .andReturn();

        assertEquals(player, mvcResultToPlayer(result));
    }

    @Test
    public void testReadPlayer_doesnt_exist() throws Exception {
        // Arrange
        given(footballService.getPlayer("p3")).willThrow(new NotFoundException("Player not found"));

        // Act and Assert
        mvc.perform(get("/players/p3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddPlayere_not_exist() throws Exception {
        // Arrange
        given(footballService.addPlayer(NEW_PLAYER)).willReturn(NEW_PLAYER);

        // Act and Assert
        MvcResult result = mvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerToJson(NEW_PLAYER)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("p3"))
                .andReturn();

        assertEquals(NEW_PLAYER, mvcResultToPlayer(result));
    }

    @Test
    public void tesAddPlayer_already_exists() throws Exception {
        // Arrange
        given(footballService.addPlayer(NEW_PLAYER)).willThrow(new AlreadyExistsException("Player already exists"));

        // Act and Assert
        mvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerToJson(NEW_PLAYER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdatePlayer_exist() throws Exception {
        // Arrange
        Player player = PLAYERS.getFirst();
        String ID = player.id();
        given(footballService.updatePlayer(ID, player)).willReturn(player);

        // Act and Assert
        MvcResult result = mvc.perform(put("/players/" + ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerToJson(player)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(ID))
                .andReturn();

        assertEquals(player, mvcResultToPlayer(result));
    }

    @Test
    public void testUpdatePlayer_already_exists() throws Exception {
        // Arrange
        Player player = PLAYERS.getFirst();
        String ID = player.id();
        given(footballService.updatePlayer(ID, player)).willThrow(new NotFoundException("Player not found"));

        // Act and Assert
        mvc.perform(put("/players/" + ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerToJson(player)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeletePlayer_exist() throws Exception {
        // Arrange
        Player player = PLAYERS.getFirst();
        String ID = player.id();

        // Act and Assert
        mvc.perform(delete("/players/" + ID))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeletePlayer_doesnt_exist() throws Exception {
        // Arrange
        String NEW_ID = NEW_PLAYER.id();
        doThrow(new NotFoundException("Player not found")).when(footballService).deletePlayer(NEW_ID);

        // Act and Assert
        mvc.perform(delete("/players/" + NEW_ID))
                .andExpect(status().isNotFound());
//        assertThrows(NotFoundException.class, () -> footballService.deletePlayer(NEW_ID));
    }
}
