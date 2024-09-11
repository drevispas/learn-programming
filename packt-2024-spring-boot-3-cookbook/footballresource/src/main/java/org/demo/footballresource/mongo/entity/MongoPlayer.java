package org.demo.footballresource.mongo.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

// Without @Getter, the service will not be able to access the fields of this class
@Getter
@Document(collection = "players")
public class MongoPlayer {

    @Id
    private String id;
    private int jerseyNumber;
    private String name;
    private String position;
    private LocalDate dateOfBirth;
    private int height;
    private int weight;
}
