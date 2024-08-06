package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.repository.JpaAlbumRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaAlbumService {

    private final JpaAlbumRepository jpaAlbumRepository;

    public List<JpaPlayer> searchAlbumPlayers(int albumId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return jpaAlbumRepository.findPageablePlayersByAlbumId(albumId, pageable).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> searchAlbumTeamPlayers(int albumId, int teamId) {
        return jpaAlbumRepository.findByAlbumIdAndTeamId(albumId, teamId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> searchPlayersNotInAlbum(int albumId) {
        return jpaAlbumRepository.findPlayersNotInAlbum(albumId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }
}
