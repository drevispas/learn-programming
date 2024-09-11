package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaTeam;
import org.demo.footballresource.jpa.service.JpaTeamService;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/jpa/teams")
@RestController
public class JpaTeamController {

    private final JpaTeamService jpaTeamService;

    @PostMapping("")
    public JpaTeam createTeam(@RequestBody String name) {
        return jpaTeamService.jpaAddTeam(name);
    }

    @GetMapping("/{id}")
    public JpaTeam getTeam(@PathVariable Integer id) {
        return jpaTeamService.jpqlReadTeam(id);
    }
}
