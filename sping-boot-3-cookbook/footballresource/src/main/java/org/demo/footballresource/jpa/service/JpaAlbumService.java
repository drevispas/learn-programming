package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.*;
import org.demo.footballresource.jpa.entity.JpaAlbumEntity;
import org.demo.footballresource.jpa.entity.JpaCardEntity;
import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.demo.footballresource.jpa.repository.JpaAlbumRepository;
import org.demo.footballresource.jpa.repository.JpaCardRepository;
import org.demo.footballresource.jpa.repository.JpaPlayerRepository;
import org.demo.footballresource.jpa.repository.JpaUserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class JpaAlbumService {

    private final JpaAlbumRepository jpaAlbumRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaPlayerRepository jpaPlayerRepository;
    private final JpaCardRepository jpaCardRepository;

    public List<JpaPlayer> searchPlayersByAlbum(int albumId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return jpaAlbumRepository.findPageablePlayersByAlbumId(albumId, pageable).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> searchPlayersByAlbumTeam(int albumId, int teamId) {
        return jpaAlbumRepository.findByAlbumIdAndTeamId(albumId, teamId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaPlayer> searchPlayersNotInAlbum(int albumId) {
        return jpaAlbumRepository.findPlayersNotInAlbum(albumId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Create a new album for the user with the given title and save it to `albums` table
    public JpaAlbum buyAlbum(Integer userId, String title) {
        JpaAlbumEntity album = new JpaAlbumEntity();
        album.setTitle(title);
        album.setExpireDate(LocalDate.now().plusYears(1));
        album.setOwner(jpaUserRepository.findById(userId).orElseThrow());
        album = jpaAlbumRepository.save(album);
        return new JpaAlbum(album.getId(), album.getTitle(), album.getOwner().getId());
    }

    // Generate as many random cards as `count` and save them to `cards` table
    // No album is assigned to the cards
    public List<JpaCard> buyCards(int userId, int count) {
        return jpaCardRepository
                .saveAll(Stream.generate(() -> {
                                    var owner = jpaUserRepository.findById(userId).orElseThrow();
                                    var players = getAvailablePlayers();
                                    var card = new JpaCardEntity();
                                    card.setOwner(owner);
                                    card.setPlayer(players.get(new Random().nextInt(players.size())));
                                    return card;
                                })
                                .limit(count)
                                .toList()
                )
                .stream()
                .map(it -> new JpaCard(it.getId(), it.getPlayer().getId(), Optional.empty(),
                        new JpaPlayer(it.getPlayer().getId(), it.getPlayer().getName(), it.getPlayer().getJerseyNumber(),
                                it.getPlayer().getPosition(), it.getPlayer().getDateOfBirth()))
                )
                .toList();
    }

    // Assign a card to an album
    // In advance, a user should have bought the album and cards
    public JpaCard addCardToAlbum(int cardId, int albumId) {
        JpaCardEntity card = jpaCardRepository.findById(cardId).orElseThrow();
        JpaAlbumEntity album = jpaAlbumRepository.findById(albumId).orElseThrow();
        card.setAlbum(album);
        return new JpaCard(card.getId(), card.getOwner().getId(), Optional.of(album.getId()),
                new JpaPlayer(card.getPlayer().getId(), card.getPlayer().getName(), card.getPlayer().getJerseyNumber(),
                        card.getPlayer().getPosition(), card.getPlayer().getDateOfBirth())
        );
    }

    // Take all unused cards of the user and distribute them to the albums that are not full
    public List<JpaCard> distributeCardsToAlbum(int userId) {
        return jpaCardRepository.distributeCardsToAlbum(userId).stream()
                .map(it -> new JpaCard(it.getId(), it.getOwner().getId(), Optional.ofNullable(it.getAlbum().getId()),
                        new JpaPlayer(it.getPlayer().getId(), it.getPlayer().getName(), it.getPlayer().getJerseyNumber(),
                                it.getPlayer().getPosition(), it.getPlayer().getDateOfBirth()))
                )
                .toList();
    }

    // Transfer a card from one user to another
    public Optional<JpaCard> transferCard(int cardId, int userId) {
        int count = jpaCardRepository.transferCard(cardId, userId);
        if (count == 0) {
            return Optional.empty();
        } else {
            JpaCardEntity card = jpaCardRepository.findById(cardId).orElseThrow();
            return Optional.of(new JpaCard(card.getId(), card.getOwner().getId(), Optional.ofNullable(card.getAlbum().getId()),
                    new JpaPlayer(card.getPlayer().getId(), card.getPlayer().getName(), card.getPlayer().getJerseyNumber(),
                            card.getPlayer().getPosition(), card.getPlayer().getDateOfBirth()))
            );
        }
    }

    // Transfer all unused cards of the user to another user
    public List<JpaCard> tradeUnusedCards(int userId1, int userId2) {
        var tradableCountFromUser1ToUser2 = jpaCardRepository.countMatchBetweenUsers(userId1, userId2);
        var tradableCountFromUser2ToUser1 = jpaCardRepository.countMatchBetweenUsers(userId2, userId1);
        int count = Math.min(tradableCountFromUser1ToUser2, tradableCountFromUser2ToUser1);
        if (count == 0) {
            return List.of();
        } else {
            var result = new ArrayList<>(jpaCardRepository.transferCardsBetweenUsers(userId1, userId2, count));
            distributeCardsToAlbum(userId2);
            result.addAll(jpaCardRepository.transferCardsBetweenUsers(userId2, userId1, count));
            distributeCardsToAlbum(userId1);
            return result.stream()
                    .map(it -> new JpaCard(it.getId(), it.getOwner().getId(), Optional.ofNullable(it.getAlbum().getId()),
                            new JpaPlayer(it.getPlayer().getId(), it.getPlayer().getName(), it.getPlayer().getJerseyNumber(),
                                    it.getPlayer().getPosition(), it.getPlayer().getDateOfBirth()))
                    )
                    .toList();
        }
    }

    @Cacheable(value = "availablePlayers")
    public List<JpaPlayerEntity> getAvailablePlayers() {
        return (List<JpaPlayerEntity>) jpaPlayerRepository.findAll();
    }

    public JpaTradingUser getUserWithCardsAndAlbums(int userId) {
        var user = jpaUserRepository.findUserWithCardsAndAlbums(userId);
        return new JpaTradingUser(
                new JpaUser(user.getId(), user.getUsername()),
                user.getOwnedCards().stream()
                        .map(it -> new JpaCard(it.getId(), it.getPlayer().getId(), Optional.ofNullable(it.getAlbum().getId()),
                                new JpaPlayer(it.getPlayer().getId(), it.getPlayer().getName(), it.getPlayer().getJerseyNumber(),
                                        it.getPlayer().getPosition(), it.getPlayer().getDateOfBirth()))
                        )
                        .toList(),
                user.getOwnedAlbums().stream()
                        .map(it -> new JpaAlbum(it.getId(), it.getTitle(), it.getOwner().getId()))
                        .toList()
        );
    }
}
