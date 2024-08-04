package org.demo.footballresource.nodb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class DataService {

    private final JdbcTemplate jdbcTemplate;

    public String getPlayerStats(String player) {
        Random random = new Random();
        jdbcTemplate.execute("SELECT pg_sleep(" + random.nextDouble(1.0) + ")");
        return "some complex stats for player " + player;
    }
}
