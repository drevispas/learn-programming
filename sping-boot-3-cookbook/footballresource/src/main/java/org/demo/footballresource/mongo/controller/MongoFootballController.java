package org.demo.footballresource.mongo.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.mongo.entity.MongoMatchEvent;
import org.demo.footballresource.mongo.entity.MongoPlayer;
import org.demo.footballresource.mongo.entity.MongoTeam;
import org.demo.footballresource.mongo.service.MongoFootballService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/mongo/football")
@RestController
public class MongoFootballController {

    private final MongoFootballService footballService;

    @GetMapping("/teams/{teamId}")
    public MongoTeam getTeamById(@PathVariable String teamId) {
        return footballService.getTeamById(teamId);
    }

    @GetMapping("/teams")
    public MongoTeam getTeamByName(@RequestParam String teamName) {
        return footballService.getTeamByName(teamName);
    }

    @GetMapping("/teams/contains")
    public List<MongoTeam> listTeamsContainingName(@RequestParam String teamName) {
        return footballService.listTeamsContainingName(teamName);
    }

    @PostMapping("/teams")
    public MongoTeam saveTeam(@RequestBody MongoTeam team) {
        return footballService.saveTeam(team);
    }

    @DeleteMapping("/teams/{teamId}")
    public void deleteTeam(@PathVariable String teamId) {
        footballService.deleteTeam(teamId);
    }

    @PatchMapping("/teams/{teamId}")
    public void updateTeamName(@PathVariable String teamId, @RequestParam String teamName) {
        footballService.updateTeamName(teamId, teamName);
    }

    @GetMapping("/players/{playerId}")
    public MongoPlayer getPlayerById(@PathVariable String playerId) {
        return footballService.getPlayerById(playerId);
    }

    @GetMapping("/matches/{matchId}/events")
    public List<MongoMatchEvent> listMatchEvents(@PathVariable String matchId) {
        return footballService.listMatchEvents(matchId);
    }

    @GetMapping("/matches/{matchId}/{playerId}/events")
    public List<MongoMatchEvent> listMatchEvents(@PathVariable String matchId, @PathVariable String playerId) {
        return footballService.listPlayerEvents(matchId, playerId);
    }
}
