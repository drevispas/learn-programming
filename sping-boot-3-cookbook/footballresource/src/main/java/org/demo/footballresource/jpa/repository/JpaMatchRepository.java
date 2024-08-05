package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaMatchEntity;
import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaMatchRepository extends JpaRepository<JpaMatchEntity, Integer> {

    // List the players of a certain match, from both teams
    @Query("select p1 from JpaMatchEntity m join m.team1 t1 join t1.players p1 where m.id = ?1 union " +
            "select p2 from JpaMatchEntity m join m.team2 t2 join t2.players p2 where m.id = ?1")
    public List<JpaPlayerEntity> findPlayersByMatchId(Integer matchId);
}
