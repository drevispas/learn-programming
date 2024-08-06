package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.repository.JpaMatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaMatchService {

    private final JpaMatchRepository jpaMatchRepository;

    public List<JpaPlayer> listMatchPlayers(int matchId) {
        return jpaMatchRepository.findMatchPlayers(matchId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }
}
