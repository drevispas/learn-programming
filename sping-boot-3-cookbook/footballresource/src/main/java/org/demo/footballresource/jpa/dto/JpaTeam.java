package org.demo.footballresource.jpa.dto;

import java.util.List;

public record JpaTeam(Integer id, String name, List<JpaPlayer> jpaPlayers) {
}
