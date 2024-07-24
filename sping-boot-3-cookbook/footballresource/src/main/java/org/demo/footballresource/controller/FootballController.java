package org.demo.footballresource.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/football")
public class FootballController {

    @GetMapping("/teams")
    public List<String> listTeams() {
        return List.of("Real Madrid", "Barcelona", "Liverpool", "Manchester City");
    }

    @PostMapping("/teams")
    public String addTeam(@RequestBody String teamName) {
        return teamName + " added";
    }
}
