package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.dto.JpaTeam;
import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.demo.footballresource.jpa.repository.JpaPlayerRepository;
import org.demo.footballresource.jpa.repository.JpaTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaFootballService {

    private final JpaTeamRepository jpaTeamRepository;
    private final JpaPlayerRepository jpaPlayerRepository;

    // Search players that contain the given name
    public List<JpaPlayer> searchPlayers(String name) {
        return jpaPlayerRepository.findByNameContaining(name).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Search players by date of birth
    public List<JpaPlayer> searchPlayersByDateOfBirth(LocalDate dateOfBirth) {
        return jpaPlayerRepository.findByDateOfBirth(dateOfBirth).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Return a team including its players
    // Two queries (team read and player lazy fetch) should be executed in a single transaction
    @Transactional(readOnly = true)
    public JpaTeam readTeam(Integer teamId) {
        // jpaTeamRepository.findById() dones not read players because of LAZY fetch type
        var teamEntity = jpaTeamRepository.findById(teamId).orElseThrow();
        return new JpaTeam(teamEntity.getId(), teamEntity.getName(),
                // teamEntity.getPlayers() read player table when the players field in teams entity is accessed
                teamEntity.getPlayers().stream()
                        .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                        .toList()
        );
    }

    // Add a new team
    public JpaTeam addTeam(String name) {
        JpaTeamEntity jpaTeamEntity = new JpaTeamEntity();
        jpaTeamEntity.setName(name);
        var team = jpaTeamRepository.save(jpaTeamEntity);
        return new JpaTeam(team.getId(), team.getName(), List.of());
    }

    // Update the position of a player
    public JpaPlayer updatePlayerPosition(Integer id, String position) {
        var playerEntity = jpaPlayerRepository.findById(id).orElseThrow();
        playerEntity.setPosition(position);
        jpaPlayerRepository.save(playerEntity);
        return new JpaPlayer(playerEntity.getId(), playerEntity.getName(), playerEntity.getJerseyNumber(), playerEntity.getPosition(), playerEntity.getDateOfBirth());
    }
}
