package org.demo.footballresource.jpa.controller;

import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaAlbum;
import org.demo.footballresource.jpa.dto.JpaCard;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.dto.JpaTradingUser;
import org.demo.footballresource.jpa.service.JpaAlbumService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    @PostMapping("/buy")
    public JpaAlbum buyAlbum(@RequestBody BuyAlbumRequest request) {
        return jpaAlbumService.buyAlbum(request.userId, request.title);
    }
    public record BuyAlbumRequest(int userId, String title) {
    }

    @PostMapping("/cards/buy")
    public List<JpaCard> buyCards(@RequestBody BuyCardsRequest request) {
        return jpaAlbumService.buyCards(request.userId, request.count);
    }
    public record BuyCardsRequest(int userId, int count) {
    }

    @PostMapping("/{albumId}/cards/{cardId}")
    public JpaCard adCardToAlbum(@PathVariable int albumId, @PathVariable int cardId) {
        return jpaAlbumService.addCardToAlbum(albumId, cardId);
    }

    @PostMapping("/cards/{cardId}/transfer")
    public Optional<JpaCard> transferCard(@PathVariable int cardId, @RequestBody TransferCardRequest request) {
        return jpaAlbumService.transferCard(cardId, request.userId);
    }
    public record TransferCardRequest(int userId) {
    }

    @PostMapping("/cards/trade")
    public List<JpaCard> tradeCardsBetweenUsers(@RequestBody TradeCardsBetweenUsersRequest request) {
        return jpaAlbumService.tradeUnusedCards(request.userId1, request.userId2);
    }
    public record TradeCardsBetweenUsersRequest(int userId1, int userId2) {
    }

    @GetMapping("/users/{userId}")
    public JpaTradingUser getTradingUser(@PathVariable int userId) {
        return jpaAlbumService.getUserWithCardsAndAlbums(userId);
    }
}
