package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.service.JpaPlayerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jpa/players")
@RestController
public class JpaPlayerController {

    private final JpaPlayerService jpaPlayerService;

    @GetMapping("/name/contains/{substring}")
    public List<JpaPlayer> listPlayersContainingName(@PathVariable String substring) {
        return jpaPlayerService.jpaSearchPlayersContainingName(substring);
    }

    @GetMapping("/birth/{date}")
    public List<JpaPlayer> listPlayersByBirthDate(@PathVariable LocalDate date) {
        return jpaPlayerService.jpaSearchPlayersByDateOfBirth(date);
    }

    @PutMapping("/{id}/position")
    public JpaPlayer updatePlayerPosition(@PathVariable Integer id, @RequestBody String position) {
        return jpaPlayerService.jpaUpdatePlayerPosition(id, position);
    }

    @GetMapping("/name/starts-with/{prefix}")
    public List<JpaPlayer> listPlayersStartingWithName(@PathVariable String prefix) {
        return jpaPlayerService.jpaSearchPlayersStartingWithName(prefix);
    }

    @GetMapping("/name/like/{like}")
    public List<JpaPlayer> listPlayersNameLike(@PathVariable String like) {
        return jpaPlayerService.jpaSearchPlayersNameLike(like);
    }

    @GetMapping("/teams/{teamId}")
    public List<JpaPlayer> listPlayersByTeamId(@PathVariable Integer teamId) {
        return jpaPlayerService.japSearchPlayersByTeamId(teamId);
    }

    @GetMapping
    public List<JpaPlayer> listPlayersInIds(@RequestParam List<Integer> playerIds) {
        return jpaPlayerService.jpqlSearchPlayersInIds(playerIds);
    }
}
