package org.demo.footballresource.mongo.dto;

import java.time.LocalDate;

public record MongoPlayer(String id, String name, Integer jerseyNumber, String position, LocalDate dateOfBirth, Integer height, Integer weight) {
}
