package org.demo.dopv11.item.operation;

import org.demo.dopv11.data.item.Author;
import org.demo.dopv11.data.item.Book;
import org.demo.dopv11.data.item.Furniture;
import org.demo.dopv11.data.item.ISBN;
import org.demo.dopv11.operation.Reservation;
import org.demo.dopv11.data.user.Email;
import org.demo.dopv11.data.user.RegisteredUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ReservationTest {

    @Test
    void testReserveBook() {
        // Arrange
        var isbn = new ISBN("978-0-395-07122-1");
        var authors = List.of(new Author("J.R.R. Tolkien"));
        var item = new Book("The Hobbit", isbn, authors);
        var email = new Email("john.doe@example.com");
        var user = new RegisteredUser("John Doe", email);

        // Act
        var result = Reservation.reserve(item, user);

        // Assert
        assertThat(result, is(true));
    }

    @Test
    void testReserveFurniture() {
        // Arrange
        var item = new Furniture("Table", "Apple");
        var email = new Email("john.doe@example..com");
        var user = new RegisteredUser("John Doe", email);

        // Act
        var result = Reservation.reserve(item, user);

        // Assert
        assertThat(result, is(false));
    }
}
