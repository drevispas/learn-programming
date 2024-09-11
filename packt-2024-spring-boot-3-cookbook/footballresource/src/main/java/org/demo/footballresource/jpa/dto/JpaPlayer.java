package org.demo.footballresource.jpa.dto;

import java.time.LocalDate;

public record JpaPlayer(Integer id, String name, Integer jerseyNumber, String position, LocalDate dateOfBirth) {
}
