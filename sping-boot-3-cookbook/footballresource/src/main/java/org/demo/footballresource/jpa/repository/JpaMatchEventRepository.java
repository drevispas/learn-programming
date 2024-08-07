package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaMatchEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaMatchEventRepository extends JpaRepository<JpaMatchEventEntity, Integer> {

    // Retrieve all events in a match of a given type
    @Query(nativeQuery = true, value = "select me.* from match_events me where me.match_id = ?1 and cast(me.details -> 'type' as int) = ?2")
    public List<JpaMatchEventEntity> findByIdIncludeType(Integer matchId, Integer type);
}
