package org.demo.album.network;

import org.demo.album.model.Player;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@FeignClient(name = "football", url = "http://localhost:8080")
public interface FootballClient {

    @RequestMapping(method=GET, value="/players")
    List<Player> getPlayers();
}
