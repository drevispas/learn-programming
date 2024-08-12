package org.demo.footballresource.jpa.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaMatchEvent;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.entity.JpaMatchEventEntity;
import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DynamicQueryService {

    private final EntityManager entityManager;

    // Dynamic query using Criteria API. Preferred for type safety and compile-time checking
    // Search players of a team by name, height, and weight
    public List<JpaPlayer> searchTeamPlayers(int teamId, Optional<String> name,
                                             Optional<Integer> minHeight, Optional<Integer> maxHeight,
                                             Optional<Integer> minWeight, Optional<Integer> maxWeight) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<JpaPlayerEntity> cq = cb.createQuery(JpaPlayerEntity.class);
        Root<JpaPlayerEntity> player = cq.from(JpaPlayerEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(player.get("team").get("id"), teamId));
        name.ifPresent(it -> predicates.add(cb.like(player.get("name"), it)));
        minHeight.ifPresent(it -> predicates.add(cb.ge(player.get("height"), it)));
        maxHeight.ifPresent(it -> predicates.add(cb.le(player.get("height"), it)));
        minWeight.ifPresent(it -> predicates.add(cb.ge(player.get("weight"), it)));
        maxWeight.ifPresent(it -> predicates.add(cb.le(player.get("weight"), it)));
        cq.where(predicates.toArray(new Predicate[0]));
        TypedQuery<JpaPlayerEntity> query = entityManager.createQuery(cq);
        return query.getResultList().stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    // Dynamic query using JPQL
    // Search events of a match in a time range
    public List<JpaMatchEvent> searchMatchEventsInRange(int matchId, Optional<LocalDateTime> minTime, Optional<LocalDateTime> maxTime) {
        // EntityManager compiles the JPQL query
        String command = "select e from JpaMatchEventEntity e where e.match.id = :matchId";
        if (minTime.isPresent() && maxTime.isPresent()) {
            command += " and e.eventTime between :minTime and :maxTime";
        } else if (minTime.isPresent()) {
            command += " and e.eventTime >= :minTime";
        } else if (maxTime.isPresent()) {
            command += " and e.eventTime <= :maxTime";
        }
        TypedQuery<JpaMatchEventEntity> query = entityManager.createQuery(command, JpaMatchEventEntity.class);
        query.setParameter("matchId", matchId);
        minTime.ifPresent(it -> query.setParameter("minTime", it));
        maxTime.ifPresent(it -> query.setParameter("maxTime", it));
        return query.getResultList().stream()
                .map(it -> new JpaMatchEvent(it.getEventTime(), it.getDetails()))
                .toList();
    }

    // Dynamic query using JPQL
    // Delete the events of a match in certain time range
    public void deleteEventsInRange(int matchId, LocalDateTime start, LocalDateTime end) {
        entityManager.getTransaction().begin();
        Query query = entityManager.createQuery("""
                delete from JpaMatchEventEntity e
                where e.match.id = :matchId and e.eventTime between :start and :end
                """);
        query.setParameter("matchId", matchId);
        query.setParameter("start", start);
        query.setParameter("end", end);
        query.executeUpdate();
        entityManager.getTransaction().commit();
    }

    // Dynamic query using Native SQL
    // Search players that a user does not own yet
    public List<JpaPlayer> searchUserMissingPlayers(int userId) {
        // EntityManager does not compile the native query
        Query query = entityManager.createNativeQuery("""
                select p.*
                from players p
                where p.id not in (
                    select c.player_id
                    from cards c
                    join albums a on c.album_id = a.id
                    where a.owner_id = :userId
                )
                """, JpaPlayerEntity.class);
        query.setParameter("userId", userId);
        return ((List<JpaPlayerEntity>) query.getResultList()).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }
}
