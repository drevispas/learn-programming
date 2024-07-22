package org.demo.football.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.demo.football.exceptions.NotFoundException;
import org.demo.football.model.Player;
import org.demo.football.services.FootballService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        List<Player> players = List.of(
                new Player("p1", 5, "Alice", "Defender", LocalDate.of(1990, 1, 1)),
                new Player("p2", 31, "Bob", "Midfielder", LocalDate.of(1994, 2, 4))
        );
        given(footballService.listPlayers()).willReturn(players);

        // Act
        // MockMvc.perform() returns a ResultActions object that provides methods like andExpect().
        MvcResult result = mvc.perform(get("/players"))
                // HTTP status code between 200 and 299
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andReturn();

        // Assert
        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        List<Player> actualPlayers = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, Player.class));
        assertArrayEquals(players.toArray(), actualPlayers.toArray());
    }

    @Test
    public void testReadPlayer_doesnt_exist() throws Exception {
        // Arrange
        given(footballService.getPlayer("p3")).willThrow(new NotFoundException("Player not found"));

        // Act and Assert
        mvc.perform(get("/players/p3"))
                .andExpect(status().isNotFound());
    }
}
