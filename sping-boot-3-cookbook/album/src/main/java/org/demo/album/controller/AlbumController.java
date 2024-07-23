package org.demo.album.controller;

import org.demo.album.network.FootballClient;
import org.demo.album.model.Player;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/albums")
public class AlbumController {

    private final FootballClient footballClient;

    public AlbumController(FootballClient footballClient) {
        this.footballClient = footballClient;
    }

    @GetMapping("/players")
    public List<Player> getPlayers() {
        return footballClient.getPlayers();
    }
}
