package org.demo.footballresource.jpa.dto;

import org.demo.footballresource.jpa.entity.JpaMatchEventDetails;

import java.time.LocalDateTime;

public record JpaMatchEvent(LocalDateTime eventTime, JpaMatchEventDetails details) {
}
