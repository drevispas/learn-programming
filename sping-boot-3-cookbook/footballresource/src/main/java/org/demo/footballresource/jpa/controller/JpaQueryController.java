package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaMatchEvent;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.service.DynamicQueryService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RequestMapping("/jpa/query")
@RestController
public class JpaQueryController {

    private final DynamicQueryService dynamicQueryService;

    @GetMapping("/teams/{teamId}/players")
    public List<JpaPlayer> searchTeamPlayers(@PathVariable int teamId,
                                             @RequestParam(required = false) Optional<String> name,
                                             @RequestParam(required = false) Optional<Integer> minHeight,
                                             @RequestParam(required = false) Optional<Integer> maxHeight,
                                             @RequestParam(required = false) Optional<Integer> minWeight,
                                             @RequestParam(required = false) Optional<Integer> maxWeight) {
        return dynamicQueryService.searchTeamPlayers(teamId, name, minHeight, maxHeight, minWeight, maxWeight);
    }

    @GetMapping("/matches/{matchId}/events")
    public List<JpaMatchEvent> searchMatchEventsInRange(@PathVariable int matchId,
                                                        @RequestParam(required = false) Optional<LocalDateTime> minTime,
                                                        @RequestParam(required = false) Optional<LocalDateTime> maxTime) {
        return dynamicQueryService.searchMatchEventsInRange(matchId, minTime, maxTime);
    }

    @GetMapping("/users/{userId}/missing")
    public List<JpaPlayer> searchMissingPlayers(@PathVariable int userId) {
        return dynamicQueryService.searchUserMissingPlayers(userId);
    }
}
