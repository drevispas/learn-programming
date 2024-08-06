package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.dto.TeamPlayers;
import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTeamRepository extends CrudRepository<JpaTeamEntity, Integer> {

    // Get a team and its players
    @Query("select t from JpaTeamEntity t join fetch t.players p where t.id = ?1")
    Optional<JpaTeamEntity> findTeamById(Integer teamId);

    // Get the number of players by position for each team
    @Query("select t.name as teamName, count(p) as playersCount from JpaPlayerEntity p join p.team t where p.position = ?1 group by p.team order by playersCount desc")
    List<TeamPlayers> countTeamPlayersByPosition(String position);
}
