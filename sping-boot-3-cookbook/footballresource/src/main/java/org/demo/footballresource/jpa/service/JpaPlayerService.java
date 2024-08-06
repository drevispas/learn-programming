package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.repository.JpaPlayerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaPlayerService {

    private final JpaPlayerRepository jpaPlayerRepository;

    // Search players that contain the given substring
    public List<JpaPlayer> jpaSearchPlayersContainingName(String substring) {
        return jpaPlayerRepository.findByNameContaining(substring).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Search players by date of birth
    public List<JpaPlayer> jpaSearchPlayersByDateOfBirth(LocalDate dateOfBirth) {
        return jpaPlayerRepository.findByDateOfBirth(dateOfBirth).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Update the position of a player
    public JpaPlayer jpaUpdatePlayerPosition(Integer id, String position) {
        var playerEntity = jpaPlayerRepository.findById(id).orElseThrow();
        playerEntity.setPosition(position);
        jpaPlayerRepository.save(playerEntity);
        return new JpaPlayer(playerEntity.getId(), playerEntity.getName(), playerEntity.getJerseyNumber(), playerEntity.getPosition(), playerEntity.getDateOfBirth());
    }

    public List<JpaPlayer> jpaSearchPlayersStartingWithName(String prefix) {
        return jpaPlayerRepository.findByNameStartingWith(prefix).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> jpaSearchPlayersNameLike(String like) {
        return jpaPlayerRepository.findByNameLike(like).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> japSearchPlayersByTeamId(Integer teamId) {
        Sort sort = Sort.by("name").ascending();
        return jpaPlayerRepository.findByTeamId(teamId, sort).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> jpqlSearchPlayersInIds(List<Integer> playerIds) {
        return jpaPlayerRepository.findPlayersInIds(playerIds).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }
}
