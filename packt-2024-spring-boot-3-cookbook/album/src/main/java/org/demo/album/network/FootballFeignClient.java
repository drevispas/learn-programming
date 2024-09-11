package org.demo.album.network;

import org.demo.album.model.Player;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

//@FeignClient(name = "football", url = "http://localhost:8080")
// Set just the name of the service to use the service discovery feature
@FeignClient(name = "football")
public interface FootballFeignClient {

    @RequestMapping(method=GET, value="/players")
    List<Player> getPlayers();

    @RequestMapping(method=GET, value="/players/{id}")
    Player getPlayer(@PathVariable String id);

    @RequestMapping(method = GET, value="/serviceinfo")
    String getServiceInfo();
}
