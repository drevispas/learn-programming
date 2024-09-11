package org.demo.footballresource.mongo.repository;

import org.demo.footballresource.mongo.entity.MongoMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoMatchRepository extends MongoRepository<MongoMatch, String> {
}
