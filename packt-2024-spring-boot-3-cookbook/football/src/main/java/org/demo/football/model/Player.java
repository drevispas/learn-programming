package org.demo.football.model;

import java.time.LocalDate;

public record Player(String id, int number, String name, String position, LocalDate dateOfBirth) {
}
