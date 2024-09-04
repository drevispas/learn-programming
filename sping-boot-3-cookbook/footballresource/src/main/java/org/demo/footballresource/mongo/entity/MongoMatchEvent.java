package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.time.LocalDateTime;
import java.util.List;

@Getter
// We are interested in sharding the collection based on the match field
// @Sharded is used to specify the shard key and this makes the workload is distributed across the servers
@Sharded(shardKey = {"match"})
@Document(collection = "match_events")
public class MongoMatchEvent {

    @Id
    private String id;
    // @Field is used to specify the field name in the document in case it is different from the field name in the entity
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
    // `match` is indexed implicitly as it is used as a shard key
    private MongoMatch match;
}
