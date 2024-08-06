package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.dto.JpaTeam;
import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.demo.footballresource.jpa.repository.JpaTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaTeamService {

    private final JpaTeamRepository jpaTeamRepository;

    // Add a new team
    public JpaTeam jpaAddTeam(String name) {
        JpaTeamEntity jpaTeamEntity = new JpaTeamEntity();
        jpaTeamEntity.setName(name);
        var team = jpaTeamRepository.save(jpaTeamEntity);
        return new JpaTeam(team.getId(), team.getName(), List.of());
    }

    // Return a team including its players
    // Two queries (team read and player lazy fetch) should be executed in a single transaction
    @Transactional(readOnly = true)
    public JpaTeam jpaReadTeam(Integer teamId) {
        // jpaTeamRepository.findById() dones not read players because of LAZY fetch type
        var teamEntity = jpaTeamRepository.findById(teamId).orElseThrow();
        return new JpaTeam(teamEntity.getId(), teamEntity.getName(),
                // teamEntity.getPlayers() read player table when the players field in teams entity is accessed
                teamEntity.getPlayers().stream()
                        .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                        .toList()
        );
    }

    // JPQL join query make sure that team and players are fetched in a single query so that @Transactional is not needed
    public JpaTeam jpqlReadTeam(Integer teamId) {
        var teamEntity = jpaTeamRepository.findTeamById(teamId).orElseThrow();
        return new JpaTeam(teamEntity.getId(), teamEntity.getName(),
                teamEntity.getPlayers().stream()
                        .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                        .toList()
        );
    }
}
