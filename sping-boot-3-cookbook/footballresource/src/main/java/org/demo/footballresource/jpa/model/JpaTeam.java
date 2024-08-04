package org.demo.footballresource.jpa.model;

import java.util.List;

public record JpaTeam(Integer id, String name, List<JpaPlayer> jpaPlayers) {
}
