package org.demo.footballresource.mongo.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.mongo.dto.MongoPlayer;
import org.demo.footballresource.mongo.entity.MongoTeam;
import org.demo.footballresource.mongo.service.MongoFootballService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/mongo")
@RestController
public class MongoFootballController {

    private final MongoFootballService footballService;

    @GetMapping("/teams/{teamId}")
    public MongoTeam getTeam(@PathVariable String teamId) {
        return footballService.getTeam(teamId);
    }

    @GetMapping("/teams")
    public MongoTeam searchTeamByName(@RequestParam String teamName) {
        return footballService.searchTeamByName(teamName);
    }

    @GetMapping("/teams/{name}/contains")
    public List<MongoTeam> getTeamsContainingName(@PathVariable String name) {
        return footballService.getTeamsContainingName(name);
    }

    @GetMapping("/players/{playerId}")
    public MongoPlayer getPlayer(@PathVariable String playerId) {
        return footballService.getPlayer(playerId);
    }

    @PostMapping("/teams")
    public MongoTeam createTeam(@RequestBody MongoTeam team) {
        return footballService.createTeam(team);
    }

    @DeleteMapping("/teams/{teamId}")
    public void deleteTeam(@PathVariable String teamId) {
        footballService.deleteTeam(teamId);
    }

    @GetMapping("/teams/{name}/sql")
    public List<MongoTeam> listTeamsByNameSQL(@PathVariable String name) {
        return footballService.listTeamsByNameSQL(name);
    }

    @PutMapping("/teams/{teamId}")
    public void updateTeamName(@PathVariable String teamId, @RequestParam String newName) {
        footballService.updateTeamName(teamId, newName);
    }
}
