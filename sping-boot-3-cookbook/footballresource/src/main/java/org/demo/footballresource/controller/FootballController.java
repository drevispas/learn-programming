package org.demo.footballresource.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.service.FileLoader;
import org.demo.footballresource.service.TradingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/football")
@RequiredArgsConstructor
public class FootballController {

    private final FileLoader fileLoader;
    private final TradingService tradingService;

    @GetMapping("/teams")
    public List<String> listTeams() {
        return fileLoader.getTeams();
    }

    @PostMapping("/teams")
    public String addTeam(@RequestBody String teamName) {
        return teamName + " added";
    }

    @PostMapping
    public int tradeCards(@RequestBody int orders) {
        return tradingService.tradeCards(orders);
    }
}
