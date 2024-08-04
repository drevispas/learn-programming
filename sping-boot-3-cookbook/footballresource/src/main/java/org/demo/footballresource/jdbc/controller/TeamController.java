package org.demo.footballresource.jdbc.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jdbc.entity.Team;
import org.demo.footballresource.jdbc.service.TeamJdbcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jdbc/teams")
@RestController
public class TeamController {

    private final TeamJdbcService teamJdbcService;

    @GetMapping("/count")
    public int countTeams() {
        return teamJdbcService.countTeams();
    }

    @GetMapping
    public List<Team> listTeams() {
        return teamJdbcService.listTeams();
    }

    @GetMapping("/{id}")
    public Team readTeam(@PathVariable int id) {
        return teamJdbcService.getTeam(id);
    }
}
