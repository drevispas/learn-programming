package org.demo.footballresource.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.service.FileLoader;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/football")
@RequiredArgsConstructor
public class FootballController {

    private final FileLoader fileLoader;

    @GetMapping("/teams")
    public List<String> listTeams() {
        return fileLoader.getTeams();
    }

    @PostMapping("/teams")
    public String addTeam(@RequestBody String teamName) {
        return teamName + " added";
    }
}
