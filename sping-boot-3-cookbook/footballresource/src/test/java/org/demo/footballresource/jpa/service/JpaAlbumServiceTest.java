package org.demo.footballresource.jpa.service;

import lombok.extern.slf4j.Slf4j;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = JpaAlbumServiceTest.Initializer.class)
class JpaAlbumServiceTest {

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
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @BeforeAll
    public static void startContainer() {
        postgreSQLContainer.start();
    }

    @Autowired
    private JpaAlbumService jpaAlbumService;

    @Autowired
    private JpaUserService jpaUserService;

    @Test
    public void testBuyAlbum() {
        // Arrange
        var user = jpaUserService.addUser("Alice");
        var albumTitle = "Album 1";

        // Act
        var album = jpaAlbumService.buyAlbum(user.id(), albumTitle);

        // Assert
        assertEquals(albumTitle, album.title());
        assertEquals(user.id(), album.ownerId());
    }

    @Test
    public void testBuyCards() {
        // Arrange
        var user = jpaUserService.addUser("Alice");

        // Act
        var cards = jpaAlbumService.buyCards(user.id(), 5);

        // Assert
        assertEquals(5, cards.size());
        log.info("\nCards:\n {}", cards);
    }

    @Test
    public void testAddCardToAlbum() {
        // Arrange
        var user = jpaUserService.addUser("Alice");
        var album = jpaAlbumService.buyAlbum(user.id(), "Album 1");
        var cards = jpaAlbumService.buyCards(user.id(), 5);

        // Act
        var card = jpaAlbumService.addCardToAlbum(cards.getFirst().id(), album.id());

        // Assert
        assertEquals(user.id(), card.ownerId());
        assertEquals(album.id(), card.albumId().get());
        assertEquals(cards.getFirst().player(), card.player());
    }

    @Test
    public void testDistributeCardsToAlbum() {
        // Arrange
        var user = jpaUserService.addUser("Alice");
        var album = jpaAlbumService.buyAlbum(user.id(), "Album 1");
        var cards = jpaAlbumService.buyCards(user.id(), 5);

        // Act
        var distributedCards = jpaAlbumService.distributeCardsToAlbum(user.id());

        // Assert
        assertEquals(5, distributedCards.size());
        distributedCards.forEach(card -> assertThat(card.albumId(), notNullValue()));
        log.info("\nDistributed cards:\n {}", distributedCards);
    }
}
