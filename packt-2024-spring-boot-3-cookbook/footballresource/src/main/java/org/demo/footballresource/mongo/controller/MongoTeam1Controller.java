package org.demo.footballresource.mongo.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.mongo.dto.MongoPlayer1Dto;
import org.demo.footballresource.mongo.entity.MongoTeam1;
import org.demo.footballresource.mongo.service.MongoTeam1Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/mongo/teams1")
@RestController
public class MongoTeam1Controller {

    private final MongoTeam1Service footballService;

    @GetMapping("/{teamId}")
    public MongoTeam1 getTeam(@PathVariable String teamId) {
        return footballService.getTeam(teamId);
    }

    @GetMapping("")
    public MongoTeam1 searchTeamByName(@RequestParam String teamName) {
        return footballService.searchTeamByName(teamName);
    }

    @GetMapping("{name}/contains")
    public List<MongoTeam1> getTeamsContainingName(@PathVariable String name) {
        return footballService.getTeamsContainingName(name);
    }

    @GetMapping("/players/{playerId}")
    public MongoPlayer1Dto getPlayer(@PathVariable String playerId) {
        return footballService.getPlayer(playerId);
    }

    @PostMapping("")
    public MongoTeam1 createTeam(@RequestBody MongoTeam1 team) {
        return footballService.createTeam(team);
    }

    @DeleteMapping("{teamId}")
    public void deleteTeam(@PathVariable String teamId) {
        footballService.deleteTeam(teamId);
    }

    @GetMapping("{name}/sql")
    public List<MongoTeam1> listTeamsByNameSQL(@PathVariable String name) {
        return footballService.listTeamsByNameSQL(name);
    }

    @PutMapping("{teamId}")
    public void updateTeamName(@PathVariable String teamId, @RequestParam String newName) {
        footballService.updateTeamName(teamId, newName);
    }
}
