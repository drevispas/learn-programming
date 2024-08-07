package org.demo.footballresource.jpa.controller;

import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.service.JpaAlbumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jpa/albums")
@RestController
public class JpaAlbumController {

    private final JpaAlbumService jpaAlbumService;

    @GetMapping("/{albumId}/players")
    public List<JpaPlayer> listAlbumPlayers(@PathVariable int albumId, @QueryParam("page") int page, @QueryParam("size") int size) {
        return jpaAlbumService.searchPlayersByAlbum(albumId, page, size);
    }

    @GetMapping("/{albumId}/teams/{teamId}/players")
    public List<JpaPlayer> listAlbumTeamPlayers(@PathVariable int albumId, @PathVariable int teamId) {
        return jpaAlbumService.searchPlayersByAlbumTeam(albumId, teamId);
    }

    @GetMapping("/{albumId}/players/not-in")
    public List<JpaPlayer> listPlayersNotInAlbum(@PathVariable int albumId) {
        return jpaAlbumService.searchPlayersNotInAlbum(albumId);
    }
}
