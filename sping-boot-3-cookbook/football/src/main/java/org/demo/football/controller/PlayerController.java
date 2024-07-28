package org.demo.football.controller;

import org.demo.football.model.Player;
import org.demo.football.model.PlayerRanking;
import org.demo.football.services.FootballService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/players")
public class PlayerController {

    private final FootballService footballService;

    public PlayerController(FootballService footballService) {
        this.footballService = footballService;
    }

    @GetMapping
    public List<Player> listPlayers() {
        return footballService.listPlayers();
    }

    @GetMapping("/{id}")
    public Player readPlayer(@PathVariable String id) {
        return footballService.getPlayer(id);
    }

    // The message converter follows the Accept header in the request.
    // Spring serializes the response as JSON by default.
    // You can configure your own serialization by overriding WebMvcConfigurer.configureMessageConverters().
    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        return footballService.addPlayer(player);
    }

    @PutMapping("/{id}")
    public Player updatePlayer(@PathVariable String id, @RequestBody Player player) {
        return footballService.updatePlayer(id, player);
    }

    @DeleteMapping("/{id}")
    public void deletePlayer(@PathVariable String id) {
        footballService.deletePlayer(id);
    }

    @GetMapping("/rankings")
    public List<PlayerRanking> listPlayerRankings() {
        return footballService.listPlayerRankings();
    }
}
