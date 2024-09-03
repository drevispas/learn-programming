package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "players")
public class MongoPlayer {

    @Id
    private String id;
}
