package org.demo.footballresource.jdbc.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jdbc.entity.JdbcPlayer;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JdbcPlayerService {

    // JPA injects the JdbcClient bean
    private final JdbcClient jdbcClient;

    public List<JdbcPlayer> listPlayers() {
        return jdbcClient.sql("SELECT * FROM players")
                .query(JdbcPlayer.class)
                .list();
    }

    public JdbcPlayer getPlayer(int id) {
        return jdbcClient.sql("SELECT * FROM players WHERE id = :id")
                // Named parameter
                .param("id", id)
                // No need to use boilerplate code like RowMapper lambda to map the result set to the Player object
                .query(JdbcPlayer.class)
                .single();
    }

    public JdbcPlayer addPlayer(JdbcPlayer jdbcPlayer) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO players (jersey_number, name, position, date_of_birth, team_id)
                VALUES (:jerseyNumber, :name, :position, :dateOfBirth, :teamId)
                """)
                .param("name", jdbcPlayer.getName())
                .param("jerseyNumber", jdbcPlayer.getJerseyNumber())
                .param("position", jdbcPlayer.getPosition())
                .param("dateOfBirth", jdbcPlayer.getDateOfBirth())
                .param("teamId", jdbcPlayer.getTeamId())
                .update(keyHolder, "id");
        jdbcPlayer.setId(keyHolder.getKey().intValue());
        return jdbcPlayer;
    }
}
