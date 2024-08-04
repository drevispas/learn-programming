package org.demo.footballresource.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.entity.Team;
import org.demo.footballresource.service.TeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/teams")
@RestController
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/count")
    public int countTeams() {
        return teamService.countTeams();
    }

    @GetMapping
    public List<Team> listTeams() {
        return teamService.listTeams();
    }

    @GetMapping("/{id}")
    public Team readTeam(@PathVariable int id) {
        return teamService.getTeam(id);
    }
}
