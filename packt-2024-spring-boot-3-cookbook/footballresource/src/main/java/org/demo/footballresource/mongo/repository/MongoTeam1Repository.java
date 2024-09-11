package org.demo.footballresource.mongo.repository;

import org.demo.footballresource.mongo.entity.MongoTeam1;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MongoTeam1Repository extends MongoRepository<MongoTeam1, String> {

    Optional<MongoTeam1> findByName(String name);

    List<MongoTeam1> findByNameContaining(String name);

    // MQL(MongoDB Query Language)
    // input example: { "_id": "xxx", "players": [ { "_id": "yyy" } ] }
    // value = "{ 'players._id' : ?0 }": find the team with the player id
    // fields = "{ 'players.$' : 1 }": only return the player with the player id.
    // - The $ sign is a placeholder for the first element that matches the query condition.
    //   In this case, `players` is an array, and the first element is returned.
    // - 1 means that the field should be included in the result.
    @Query(value = "{ 'players._id' : ?0 }", fields = "{ 'players.$' : 1 }")
    Optional<MongoTeam1> findPlayerById(String id);

    @Query("select t from teams1 t where t.name = ?0")
    List<MongoTeam1> findTeamsByNameSQL(String name);
}
