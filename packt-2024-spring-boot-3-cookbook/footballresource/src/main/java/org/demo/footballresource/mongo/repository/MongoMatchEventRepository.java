package org.demo.footballresource.mongo.repository;

import org.demo.footballresource.mongo.entity.MongoMatchEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MongoMatchEventRepository extends MongoRepository<MongoMatchEvent, String> {

    @Query(value = "{'match.$id': ?0}")
    List<MongoMatchEvent> findByMatchId(String matchId);

    @Query(value = "{'$and': [{'match.$id': ?0}, {'$or': [{'player1.$id': ?1},{'player2.$id': ?1}]}]}")
    List<MongoMatchEvent> findByMatchIdAndPlayerId(String matchId, String playerId);
}
