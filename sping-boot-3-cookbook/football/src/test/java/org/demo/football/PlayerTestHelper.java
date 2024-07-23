package org.demo.football;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.demo.football.model.Player;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;

public class PlayerTestHelper {

    public static final List<Player> PLAYERS = List.of(
            new Player("p1", 5, "Alice", "Defender", LocalDate.parse("1990-01-01")),
            new Player("p2", 31, "Bob", "Midfielder", LocalDate.parse("1994-02-04"))
    );

    public static final Player NEW_PLAYER = new Player("p3", 10, "Charlie", "Forward", LocalDate.parse("1995-03-05"));

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static Player jsonToPlayer(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Player.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to Player", e);
        }
    }

    public static String playerToJson(Player player) {
        try {
            return OBJECT_MAPPER.writeValueAsString(player);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Player to JSON", e);
        }
    }

    public static Player mvcResultToPlayer(MvcResult result) {
        try {
            return jsonToPlayer(result.getResponse().getContentAsString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to convert MvcResult to Player", e);
        }
    }

    public static List<Player> mvcResultToPlayers(MvcResult result) {
        try {
            return OBJECT_MAPPER.readValue(
                    result.getResponse().getContentAsString(),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Player.class)
            );
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to convert MvcResult to List<Player>", e);
        }
    }
}
