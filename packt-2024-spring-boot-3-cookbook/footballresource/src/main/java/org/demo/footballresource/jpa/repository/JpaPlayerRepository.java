package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaPlayerRepository extends CrudRepository<JpaPlayerEntity, Integer> {

    List<JpaPlayerEntity> findByDateOfBirth(LocalDate dateOfBirth);
    List<JpaPlayerEntity> findByNameContaining(String name);
    // Find the players whose name starts with the given prefix
    List<JpaPlayerEntity> findByNameStartingWith(String prefix);
    // Find the players whose name contains the given substring
    List<JpaPlayerEntity> findByNameLike(String substring);
    // Sort the players of a team. For instance, .findByTeamId(id, Sort.by("name").ascending())
    List<JpaPlayerEntity> findByTeamId(Integer teamId, Sort sort);

    // Find the players matched with the request player ids
    @Query("SELECT p FROM JpaPlayerEntity p WHERE p.id IN (:playerIds)")
    List<JpaPlayerEntity> findPlayersInIds(List<Integer> playerIds);
}
