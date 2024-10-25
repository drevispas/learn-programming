package com.thehecklers.planefinder;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

@AllArgsConstructor
@Configuration
public class PositionReporter {
    private final PlaneFinderService service;
    @Bean
    Supplier<Iterable<Aircraft>> reportPositions() {
        return () -> {
            try {
                Thread.sleep(5000);
                return service.getAircraft();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return List.of();
        };
    }
}
