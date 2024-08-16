package org.demo.footballresource.mongo.service;

import lombok.extern.slf4j.Slf4j;
import org.demo.footballresource.mongo.entity.MongoTeam;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


@Slf4j
@Testcontainers
@SpringBootTest
class MongoFootballServiceTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo")
            .withCopyFileToContainer(MountableFile.forClasspathResource("mongo/teams.json"), "teams.json")
            // log the output of the container
//            .withLogConsumer(outputFrame -> log.warn(outputFrame.getUtf8String()))
            ;

    // By default, the database name is "test"
    private static final String DATABASE_NAME = "test";
    private static final int TEAM_COUNT = 32;
    private static final String TEAM_ID = "1885010";
    private static final String TEAM_NAME = "Korea Republic";
    private static final String PLAYER_ID = "420221";
    private static final String PLAYER_NAME = "KIM Yunji";

    @BeforeAll
    static void startContainer() throws IOException, InterruptedException {
        mongoDBContainer.start();
        importFile("teams");
    }

    static void importFile(String collectionName) throws IOException, InterruptedException {
        MongoDBContainer.ExecResult result = mongoDBContainer.execInContainer(
                "mongoimport", "--db=" + DATABASE_NAME, "--collection=" + collectionName, "--file=" + collectionName + ".json" , "--jsonArray"
        );
        if (result.getExitCode() != 0) {
            throw new RuntimeException("MongoDB failed to import file " + collectionName);
        }
    }

    // Configure the service to use the test container
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        System.out.println("MongoDB URL: " + mongoDBContainer.getReplicaSetUrl());
        String uri = mongoDBContainer.getReplicaSetUrl().replace("test", DATABASE_NAME);
        registry.add("spring.data.mongodb.uri", () -> uri);
    }

    @Autowired
    private MongoFootballService service;

    @Test
    void testListAllTeams() {
        // Act
        var teams = service.listAllTeams();

        // Assert
        assertThat(teams, not(nullValue()));
        assertThat(teams, not(empty()));
        assertThat(teams, hasSize(TEAM_COUNT));
    }

    @Test
    void testGetTeam() {
        // Act
        var team = service.getTeam(TEAM_ID);
        log.info("Team: {}", team);

        // Assert
        assertThat(team, not(nullValue()));
        assertThat(team.getId(), is(TEAM_ID));
    }

    @Test
    void TestGetTeam_notExists() {
        // Act
        var team = service.getTeam("123456");

        // Assert
        assertThat(team, nullValue());
    }

    @Test
    void testSearchTeamByName() {
        // Act
        var team = service.searchTeamByName(TEAM_NAME);

        // Assert
        assertThat(team, not(nullValue()));
        assertThat(team.getName(), is(TEAM_NAME));
    }

    @Test
    void testSearchTeamByName_notExists() {
        // Act
        var team = service.searchTeamByName("NotExists");

        // Assert
        assertThat(team, nullValue());
    }

    @Test
    void testGetTeamsContainingName() {
        // Act
        var teams = service.getTeamsContainingName("land");

        // Assert
        assertThat(teams, not(nullValue()));
        assertThat(teams, not(empty()));
        assertThat(teams, hasSize(5));
    }

    @Test
    void testGetTeamsContainingName_notExists() {
        // Act
        var teams = service.getTeamsContainingName("NotExists");

        // Assert
        assertThat(teams, not(nullValue()));
        assertThat(teams, empty());
    }

    @Test
    void testGetPlayer() {
        // Act
        var player = service.getPlayer(PLAYER_ID);

        // Assert
        assertThat(player, notNullValue());
        assertThat(player.id(), is(PLAYER_ID));
        assertThat(player.name(), is(PLAYER_NAME));
    }

    @Test
    void testGetPlayer_notExists() {
        // Act
        var player = service.getPlayer("12345");

        // Assert
        assertThat(player, nullValue());
    }

    @Test
    void testCreateTeam() {
        // Arrange
        var team = new MongoTeam();
        team.setName("Test Team");

        // Act
        var created = service.createTeam(team);

        // Assert
        assertNotNull(created);
        assertThat(created.getName(), is("Test Team"));

        // Clean up
        service.deleteTeam(created.getId());
    }

    @Test
    void testDeleteTeam() {
        // Arrange
        var team = new MongoTeam();
        team.setName("Test Team");
        var created = service.createTeam(team);

        // Act
        service.deleteTeam(created.getId());

        // Assert
        var deleted = service.getTeam(created.getId());
        assertThat(deleted, nullValue());
    }

    @Test
    void updateTeamName() {
        // Arrange
        var team = new MongoTeam();
        team.setName("Test Team");
        var created = service.createTeam(team);

        // Act
        service.updateTeamName(created.getId(), "Updated Team");

        // Assert
        var updated = service.getTeam(created.getId());
        assertThat(updated.getName(), is("Updated Team"));

        // Clean up
        service.deleteTeam(created.getId());
    }
}
