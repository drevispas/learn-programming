package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@ToString
@Getter
@Setter
// @Document is used to specify the collection name
@Document(collection = "teams")
public class MongoTeam {

    @Id
    private String id;
    private String name;
}
