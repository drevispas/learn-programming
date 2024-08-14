package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.ToString;
import org.demo.footballresource.mongo.dto.MongoPlayer;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@ToString
@Getter
@Document(collection = "teams")
public class MongoTeam {

    @Id
    private String id;
    private String name;
    private List<MongoPlayer> players;
}
