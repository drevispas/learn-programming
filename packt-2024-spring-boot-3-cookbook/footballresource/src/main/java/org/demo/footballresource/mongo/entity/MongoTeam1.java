package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.demo.footballresource.mongo.dto.MongoPlayer1Dto;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@ToString
@Getter
@Setter
// @Document is used to specify the collection name
@Document(collection = "teams1")
public class MongoTeam1 {

    @Id
    private String id;
    private String name;
    private List<MongoPlayer1Dto> players;
}
