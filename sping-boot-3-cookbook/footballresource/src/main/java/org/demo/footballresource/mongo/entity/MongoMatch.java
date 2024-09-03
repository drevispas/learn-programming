package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "matches")
public class MongoMatch {

    @Id
    private String id;
    private LocalDate matchDate;
    @Indexed
    @DBRef(lazy = false)
    private MongoTeam team1;
    @Indexed
    @DBRef(lazy = false)
    private MongoTeam team2;
    private int team1Goals;
    private int team2Goals;
}
