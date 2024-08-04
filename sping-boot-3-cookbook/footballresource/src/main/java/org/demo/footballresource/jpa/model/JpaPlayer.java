package org.demo.footballresource.jpa.model;

import java.time.LocalDate;

public record JpaPlayer(Integer id, String name, Integer jerseyNumber, String position, LocalDate dateOfBirth) {
}
