package org.demo.footballresource.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.entity.Player;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PlayerService {

    // JPA injects the JdbcClient bean
    private final JdbcClient jdbcClient;

    public List<Player> listPlayers() {
        return jdbcClient.sql("SELECT * FROM players")
                .query(Player.class)
                .list();
    }

    public Player getPlayer(int id) {
        return jdbcClient.sql("SELECT * FROM players WHERE id = :id")
                // Named parameter
                .param("id", id)
                // No need to use boilerplate code like RowMapper lambda to map the result set to the Player object
                .query(Player.class)
                .single();
    }

    public Player addPlayer(Player player) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO players (jersey_number, name, position, date_of_birth, team_id)
                VALUES (:jerseyNumber, :name, :position, :dateOfBirth, :teamId)
                """)
                .param("name", player.getName())
                .param("jerseyNumber", player.getJerseyNumber())
                .param("position", player.getPosition())
                .param("dateOfBirth", player.getDateOfBirth())
                .param("teamId", player.getTeamId())
                .update(keyHolder, "id");
        player.setId(keyHolder.getKey().intValue());
        return player;
    }
}
