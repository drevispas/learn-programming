package org.demo.album.controller;

import org.demo.album.model.Player;
import org.demo.album.service.FootballClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/albums")
public class AlbumController {

    private final FootballClientService footballClientService;

    public AlbumController(FootballClientService footballClientService) {
        this.footballClientService = footballClientService;
    }

    @GetMapping("/players")
    public List<Player> getPlayers() {
        return footballClientService.getPlayers();
    }

    @GetMapping("/players/{id}")
    public Optional<Player> getPlayer(@PathVariable String id) {
        return footballClientService.getPlayer(id);
    }

    @GetMapping("/feign/players")
    public List<Player> getPlayersWithFeign() {
        return footballClientService.getPlayersWithFeign();
    }

    @GetMapping("/feign/players/{id}")
    public Player getPlayerWithFeign(@PathVariable String id) {
        return footballClientService.getPlayerWithFeign(id);
    }

    @GetMapping("/serviceinfo")
    public String getServiceInfo() {
        return footballClientService.getServiceInfo();
    }
}
