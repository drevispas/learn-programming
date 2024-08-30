package org.demo.dopv11.item;

import java.util.List;
import java.util.Objects;

// records just offer access to data with little to no behavior.
public record Book(String title, ISBN isbn, List<Author> authors) implements Item {

    // This is a compact constructor
    public Book {
        // validate the state of the object
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        Objects.requireNonNull(isbn, "ISBN cannot be null");
        Objects.requireNonNull(authors, "Authors cannot be null");
        if (authors.isEmpty()) {
            throw new IllegalArgumentException("Authors cannot be empty");
        }

        // initializes the authors field to an unmodifiable list
        authors = List.copyOf(authors);
        // clients cannot modify the internal `isbn` through the parameter `isbn`.
        isbn = new ISBN(isbn);
    }

    // as a transparent carrier, OK to return just the records' internal data.
    // make sure nobody has a reference to the internal `isbn` object so that it cannot be modified.
    @Override
    public ISBN isbn() {
        return new ISBN(isbn);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Book book && isbn.equals(book.isbn));
    }

    @Override
    public int hashCode() {
        return isbn.hashCode();
    }
}
