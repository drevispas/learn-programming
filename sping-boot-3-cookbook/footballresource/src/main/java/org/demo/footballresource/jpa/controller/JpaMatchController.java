package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.entity.JpaMatchEventDetails;
import org.demo.footballresource.jpa.entity.JpaMatchEventEntity;
import org.demo.footballresource.jpa.service.JpaMatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/jpa/matches")
@RestController
public class JpaMatchController {

    private final JpaMatchService jpaMatchService;

    @GetMapping("/{matchId}/players")
    public List<JpaPlayer> listMatchPlayers(@PathVariable int matchId) {
        return jpaMatchService.listMatchPlayers(matchId);
    }

    @GetMapping("/{matchId}/types/{type}")
    public List<JpaMatchEventDetails> listMatchEventsByMatchIdAndType(@PathVariable int matchId, @PathVariable int type) {
        return jpaMatchService.listMatchEventsByMatchIdAndType(matchId, type);
    }
}
