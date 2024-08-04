package org.demo.footballresource.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.entity.Player;
import org.demo.footballresource.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/players")
@RestController
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public List<Player> listPlayers() {
        return playerService.listPlayers();
    }

    @GetMapping("/{id}")
    public Player readPlayer(@PathVariable int id) {
        return playerService.getPlayer(id);
    }

    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        return playerService.addPlayer(player);
    }
}
