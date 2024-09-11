package org.demo.footballresource.jpa.dto;

import java.util.Optional;

public record JpaCard(int id, int ownerId, Optional<Integer> albumId, JpaPlayer player) {
}
