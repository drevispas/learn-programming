package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.dto.JpaTeam;
import org.demo.footballresource.jpa.service.JpaFootballService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jpql")
@RestController
public class JpqlFootballController {

    private final JpaFootballService jpaFootballService;

    @GetMapping("/teams/{id}")
    public JpaTeam getTeam(@PathVariable Integer id) {
        return jpaFootballService.jpqlReadTeam(id);
    }
}
