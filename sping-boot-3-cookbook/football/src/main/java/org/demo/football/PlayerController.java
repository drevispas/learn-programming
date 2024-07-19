package org.demo.football;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/players")
public class PlayerController {

    @GetMapping
    public List<String> listPlayers() {
        return List.of("Ronaldo", "Messi", "Neymar");
    }

    @PostMapping
    public String createPlayer(@RequestBody String player) {
        return "Player created: " + player;
    }

    @GetMapping("/{player}")
    public String readPlayer(@PathVariable String player) {
        return "Player read: " + player;
    }

    @DeleteMapping("/{player}")
    public String deletePlayer(@PathVariable String player) {
        return "Player deleted: " + player;
    }
}
