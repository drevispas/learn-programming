package org.demo.album.model;

import java.time.LocalDate;

public record Player(
        String id,
        Integer integer,
        String name,
        String position,
        LocalDate dateOfBirth
) {
}
