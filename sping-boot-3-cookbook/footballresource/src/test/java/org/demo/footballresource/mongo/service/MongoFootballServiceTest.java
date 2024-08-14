package org.demo.footballresource.mongo.service;

import lombok.extern.slf4j.Slf4j;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
@Testcontainers
@SpringBootTest
class MongoFootballServiceTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo")
            .withCopyFileToContainer(MountableFile.forClasspathResource("mongo/teams.json"), "teams.json")
            // log the output of the container
//            .withLogConsumer(outputFrame -> log.warn(outputFrame.getUtf8String()))
            ;

    @BeforeAll
    static void startContainer() throws IOException, InterruptedException {
        mongoDBContainer.start();
        importFile("teams");
    }

    static void importFile(String collectionName) throws IOException, InterruptedException {
//        MongoDBContainer.ExecResult result = mongoDBContainer.execInContainer(
//                "mongoimport", "--db=football", "--collection=" + collectionName, "--file=" + collectionName + ".json" , "--jsonArray"
//        );
        MongoDBContainer.ExecResult result = mongoDBContainer.execInContainer(
                "mongoimport", "-d", "football", "-c", collectionName, "--file", collectionName + ".json", "--jsonArray"
        );
        if (result.getExitCode() != 0) {
            throw new RuntimeException("MongoDB failed to import file " + collectionName);
        }
    }

    private static final String TEAM_ID = "1885010";

    // Configure the service to use the test container
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MongoFootballService service;

    @Test
    void testListAllTeams() {
        // Arrange

        // Act
        var teams = service.listAllTeams();
        log.info("Teams: {}", teams);

        // Assert
        assertNotNull(teams);
        assertThat(teams).isNotEmpty();
    }

//    @Test
//    void testGetTeam() {
//        // Arrange
//
//        // Act
//        var team = service.getTeam(TEAM_ID);
//        log.info("Team: {}", team);
//
//        // Assert
//        assertNotNull(team);
//        assertThat(team.getId()).isEqualTo(TEAM_ID);
//    }
}
