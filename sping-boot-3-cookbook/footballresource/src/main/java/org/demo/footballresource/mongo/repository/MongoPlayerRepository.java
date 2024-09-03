package org.demo.footballresource.mongo.repository;

import org.demo.footballresource.mongo.entity.MongoPlayer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoPlayerRepository extends MongoRepository<MongoPlayer, String> {
}
