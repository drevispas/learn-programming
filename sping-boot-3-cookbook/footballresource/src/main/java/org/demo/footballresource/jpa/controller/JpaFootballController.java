package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.dto.JpaTeam;
import org.demo.footballresource.jpa.service.JpaFootballService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jpa")
@RestController
public class JpaFootballController {

    private final JpaFootballService jpaFootballService;

    @GetMapping("/teams/{id}")
    public JpaTeam getTeam(@PathVariable Integer id) {
        return jpaFootballService.readTeam(id);
    }

    @PostMapping("/teams")
    public JpaTeam addTeam(@RequestBody String name) {
        return jpaFootballService.addTeam(name);
    }

    @GetMapping("/players")
    public List<JpaPlayer> searchPlayers(@RequestParam String name) {
        return jpaFootballService.searchPlayers(name);
    }

    @GetMapping("/players/birth/{date}")
    public List<JpaPlayer> searchPlayersByBirthDate(@PathVariable LocalDate date) {
        return jpaFootballService.searchPlayersByDateOfBirth(date);
    }

    @PutMapping("/players/{id}/position")
    public JpaPlayer updatePlayerPosition(@PathVariable Integer id, @RequestBody String position) {
        return jpaFootballService.updatePlayerPosition(id, position);
    }
}
