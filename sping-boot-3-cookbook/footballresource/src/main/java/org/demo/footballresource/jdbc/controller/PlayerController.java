package org.demo.footballresource.jdbc.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jdbc.entity.Player;
import org.demo.footballresource.jdbc.service.PlayerJdbcService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jdbc/players")
@RestController
public class PlayerController {

    private final PlayerJdbcService playerJdbcService;

    @GetMapping
    public List<Player> listPlayers() {
        return playerJdbcService.listPlayers();
    }

    @GetMapping("/{id}")
    public Player readPlayer(@PathVariable int id) {
        return playerJdbcService.getPlayer(id);
    }

    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        return playerJdbcService.addPlayer(player);
    }
}
