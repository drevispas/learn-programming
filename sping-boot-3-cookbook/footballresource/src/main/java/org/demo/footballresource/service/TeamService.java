package org.demo.footballresource.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.entity.Team;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TeamService {

    // JPA injects the JdbcTemplate bean
    private final JdbcTemplate jdbcTemplate;

    public int countTeams() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams", Integer.class);
    }

    public List<Team> listTeams() {
        // query() gets RowMapper as a lambda to map the result set to the Team object
        return jdbcTemplate.query("SELECT * FROM teams", (rs, rowNum) -> {
            Team team = new Team();
            team.setId(rs.getInt("id"));
            team.setName(rs.getString("name"));
            return team;
        });
    }

    public Team getTeam(int id) {
        // queryForObject() to get a single row
        // BeanPropertyRowMapper also maps the result set to the Team object
        return jdbcTemplate.queryForObject("SELECT * FROM teams WHERE id = ?", new BeanPropertyRowMapper<>(Team.class), id);
    }
}
