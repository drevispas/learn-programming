package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JpaTeamRepository extends CrudRepository<JpaTeamEntity, Integer> {

    // Get a team and its players
    @Query("select t from JpaTeamEntity t join fetch t.players p where t.id = ?1")
    public Optional<JpaTeamEntity> findTeamAndPlayersById(Integer teamId);
}
