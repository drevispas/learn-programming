package org.demo.footballresource.jpa.dto;

import java.util.List;

public record JpaTradingUser(JpaUser user, List<JpaCard> cards, List<JpaAlbum> albums) {
}
