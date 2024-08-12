package org.demo.footballresource.jpa.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

@ContextConfiguration(initializers = DynamicQueryServiceTest.Initializer.class)
@Testcontainers
@SpringBootTest
class DynamicQueryServiceTest {

    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("football")
            .withUsername("user")
            .withPassword("user");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                            "spring.datasource.password=" + postgreSQLContainer.getPassword())
                    .applyTo(configurableApplicationContext.getEnvironment()
                    );
        }
    }

    @BeforeAll
    public static void startContainer() {
        postgreSQLContainer.start();
    }

    @Autowired
    private DynamicQueryService dynamicQueryService;

    @Test
    public void testSearchTeamPlayers() {
        // Arrange
        final int TEAM_ID = 1885010;
        final int TEAM_SIZE = 23;

        // Act and Assert
        var players = dynamicQueryService.searchTeamPlayers(TEAM_ID, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(TEAM_SIZE, players.size());

        players = dynamicQueryService.searchTeamPlayers(TEAM_ID, Optional.of("XX"),
                Optional.of(10), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(players, empty());

        players = dynamicQueryService.searchTeamPlayers(TEAM_ID, Optional.empty(),
                Optional.of(170), Optional.of(180), Optional.empty(), Optional.empty());
        assertThat(players, not(empty()));
    }

    @Test
    public void testSearchMatchEventsInRange() {
        // Arrange
        final int MATCH_ID = 400222852;
        final int EVENT_SIZE = 338;

        // Act and Assert
        var events = dynamicQueryService.searchMatchEventsInRange(MATCH_ID, Optional.empty(), Optional.empty());
        assertEquals(EVENT_SIZE, events.size());

        events = dynamicQueryService.searchMatchEventsInRange(MATCH_ID, Optional.of(LocalDateTime.of(2023,7,20,7,2,0)),
                Optional.of(LocalDateTime.of(2023,7,20,7,4,0)));
        assertThat(events, not(empty()));
        assertThat(events, hasSize(3));
    }
}
