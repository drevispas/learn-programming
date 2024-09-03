package org.demo.footballresource.mongo.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.mongo.dto.MongoPlayer1Dto;
import org.demo.footballresource.mongo.entity.MongoTeam1;
import org.demo.footballresource.mongo.service.MongoTeam1Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/mongo")
@RestController
public class MongoFootballController {

    private final MongoTeam1Service footballService;

    @GetMapping("/teams/{teamId}")
    public MongoTeam1 getTeam(@PathVariable String teamId) {
        return footballService.getTeam(teamId);
    }

    @GetMapping("/teams")
    public MongoTeam1 searchTeamByName(@RequestParam String teamName) {
        return footballService.searchTeamByName(teamName);
    }

    @GetMapping("/teams/{name}/contains")
    public List<MongoTeam1> getTeamsContainingName(@PathVariable String name) {
        return footballService.getTeamsContainingName(name);
    }

    @GetMapping("/players/{playerId}")
    public MongoPlayer1Dto getPlayer(@PathVariable String playerId) {
        return footballService.getPlayer(playerId);
    }

    @PostMapping("/teams")
    public MongoTeam1 createTeam(@RequestBody MongoTeam1 team) {
        return footballService.createTeam(team);
    }

    @DeleteMapping("/teams/{teamId}")
    public void deleteTeam(@PathVariable String teamId) {
        footballService.deleteTeam(teamId);
    }

    @GetMapping("/teams/{name}/sql")
    public List<MongoTeam1> listTeamsByNameSQL(@PathVariable String name) {
        return footballService.listTeamsByNameSQL(name);
    }

    @PutMapping("/teams/{teamId}")
    public void updateTeamName(@PathVariable String teamId, @RequestParam String newName) {
        footballService.updateTeamName(teamId, newName);
    }
}
