package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.time.LocalDateTime;
import java.util.List;

@Sharded(shardKey = {"match"})
@Document(collection = "matchEvents")
public class MongoMatchEvent {

    @Id
    private String id;
    @Field(name = "event_time")
    private LocalDateTime time;
    private int type;
    private String description;
    @Indexed
    @DBRef
    private MongoPlayer player1;
    @Indexed
    @DBRef
    private MongoPlayer player2;
    private List<String> mediaFiles;
    @DBRef
    private MongoMatch match;
}
