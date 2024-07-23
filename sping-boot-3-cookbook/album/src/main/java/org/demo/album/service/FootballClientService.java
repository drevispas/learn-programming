package org.demo.album.service;

import lombok.extern.slf4j.Slf4j;
import org.demo.album.model.Player;
import org.demo.album.network.FootballClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FootballClientService {

    // footballClient is a FeignClient
    private final FootballClient footballClient;
    private final RestClient restClient;
    public FootballClientService(FootballClient footballClient, RestClient restClient) {
        this.footballClient = footballClient;
        this.restClient = restClient;
    }

    public List<Player> getPlayersWithFeign() {
        return footballClient.getPlayers();
    }

    public Player getPlayerWithFeign(String id) {
        return footballClient.getPlayer(id);
    }

    public List<Player> getPlayers() {
        // body() requires an instance of ParameterizedTypeReference to specify the type of the response
        return restClient.get().uri("/players").retrieve().body(new ParameterizedTypeReference<>() {
        });
    }

    public Optional<Player> getPlayer(String id) {
        return restClient.get().uri("/players/{id}", id)
                // exchange() calls the API and provides a handler for the response
                .exchange((request, response) -> {
                    if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(response.bodyTo(Player.class));
                });
    }
}
