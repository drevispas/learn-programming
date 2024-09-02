package org.demo.jooq;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.is;

@SpringBootTest
@Testcontainers
public class JooqTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("football")
            .withUsername("user")
            .withPassword("user");

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        dsl.execute("CREATE TABLE IF NOT EXISTS author (author_id SERIAL PRIMARY KEY, name VARCHAR(255))");
        dsl.execute("CREATE TABLE IF NOT EXISTS book (book_id SERIAL PRIMARY KEY, title VARCHAR(255))");
        dsl.execute("CREATE TABLE IF NOT EXISTS book_author (book_id INT, author_id INT, PRIMARY KEY (book_id, author_id))");
    }

    @AfterEach
    void tearDown() {
        dsl.execute("DROP TABLE IF EXISTS book_author");
        dsl.execute("DROP TABLE IF EXISTS book");
        dsl.execute("DROP TABLE IF EXISTS author");
    }

    @Test
    void testJooqQuery() {
        // Insert a user, an album, and a purchase
        dsl.insertInto(DSL.table("author"), DSL.field("name"))
                .values("Alice")
                .execute();
        dsl.insertInto(DSL.table("book"), DSL.field("title"))
                .values("Album 1")
                .execute();
        dsl.insertInto(DSL.table("book_author"), DSL.field("author_id"), DSL.field("book_id"))
                .values(1, 1)
                .execute();

        // Select the user's name and the album's title
        var result = dsl.select(DSL.field("a.name"), DSL.field("b.title"))
                .from(DSL.table("author").as("a"))
                .join(DSL.table("book_author").as("ba")).on(DSL.field("a.author_id").eq(DSL.field("ba.author_id")))
                .join(DSL.table("book").as("b")).on(DSL.field("ba.book_id").eq(DSL.field("b.book_id")))
                .fetch();

        // Assert that the result has one record using hamcrest
        assertThat(result.size(), is(1));
        // Assert that the user's name is "Alice" and the album's title is "Album 1"
        assertThat(result.get(0).get(DSL.field("a.name")), is("Alice"));
        assertThat(result.get(0).get(DSL.field("b.title")), is("Album 1"));
    }
}
