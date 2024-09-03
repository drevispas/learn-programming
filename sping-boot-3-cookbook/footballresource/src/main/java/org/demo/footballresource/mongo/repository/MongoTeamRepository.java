package org.demo.footballresource.mongo.repository;

import org.demo.footballresource.mongo.entity.MongoTeam;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MongoTeamRepository extends MongoRepository<MongoTeam, String> {

    Optional<MongoTeam> findByName(String name);

    List<MongoTeam> findByNameContaining(String name);
}
