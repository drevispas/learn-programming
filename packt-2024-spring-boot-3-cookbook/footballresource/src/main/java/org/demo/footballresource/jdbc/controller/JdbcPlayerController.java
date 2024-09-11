package org.demo.footballresource.jdbc.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jdbc.entity.JdbcPlayer;
import org.demo.footballresource.jdbc.service.JdbcPlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jdbc/players")
@RestController
public class JdbcPlayerController {

    private final JdbcPlayerService jdbcPlayerService;

    @GetMapping
    public List<JdbcPlayer> listPlayers() {
        return jdbcPlayerService.listPlayers();
    }

    @GetMapping("/{id}")
    public JdbcPlayer readPlayer(@PathVariable int id) {
        return jdbcPlayerService.getPlayer(id);
    }

    @PostMapping
    public JdbcPlayer createPlayer(@RequestBody JdbcPlayer jdbcPlayer) {
        return jdbcPlayerService.addPlayer(jdbcPlayer);
    }
}
