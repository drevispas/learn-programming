package org.demo.footballresource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/football")
public class FootballController {

    @GetMapping("/teams")
    public List<String> listTeams() {
        return List.of("Real Madrid", "Barcelona", "Liverpool", "Manchester City");
    }
}
