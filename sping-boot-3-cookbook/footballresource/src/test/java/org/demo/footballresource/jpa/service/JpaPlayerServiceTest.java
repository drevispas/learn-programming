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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;


@ContextConfiguration(initializers = JpaPlayerServiceTest.Initializer.class)
@Testcontainers
@SpringBootTest
class JpaPlayerServiceTest {

    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("football")
            .withUsername("user")
            .withPassword("user");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        // Override the data source properties using the testcontainers
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword())
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    // The container is started only once and shared between test methods
    // If a container is started as an instance variable, it would be started for each test method
    @BeforeAll
    public static void startContainer() {
        postgreSQLContainer.start();
    }

    @Autowired
    private JpaPlayerService jpaPlayerService;

    @Test
    public void testAddTeam() {
        // Arrange
        String teamName = "Team1";
        // Act
        var team1 = jpaPlayerService.addTeam(teamName);
        // Assert
        var team2 = jpaPlayerService.jpaReadTeam(team1.id());
        assertThat(team2, notNullValue());
        assertThat(team2, is(team1));
    }

    @Test
    public void testGetTeam() {
        // Arrange
        String teamName = "Team1";
        var team1 = jpaPlayerService.addTeam(teamName);
        // Act
        var team2 = jpaPlayerService.jpaReadTeam(team1.id());
        // Assert
        assertThat(team2, notNullValue());
        assertThat(team2, is(team1));
    }
}
