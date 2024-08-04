package org.demo.footballresource.jdbc.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jdbc.entity.JdbcTeam;
import org.demo.footballresource.jdbc.service.JdbcTeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jdbc/teams")
@RestController
public class JdbcTeamController {

    private final JdbcTeamService jdbcTeamService;

    @GetMapping("/count")
    public int countTeams() {
        return jdbcTeamService.countTeams();
    }

    @GetMapping
    public List<JdbcTeam> listTeams() {
        return jdbcTeamService.listTeams();
    }

    @GetMapping("/{id}")
    public JdbcTeam readTeam(@PathVariable int id) {
        return jdbcTeamService.getTeam(id);
    }
}
