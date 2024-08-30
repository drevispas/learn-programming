package org.demo.dopv11.item;

import java.util.List;

public record Book(String title, ISBN isbn, List<Author> authors) implements Item {

    // This is a compact constructor that initializes the authors field to an unmodifiable list.
    public Book {
        authors = List.copyOf(authors);
        // clients cannot modify the internal `isbn` through the parameter `isbn`.
        isbn = new ISBN(isbn);
    }

    // Make sure nobody has a reference to the internal `isbn` object so that it cannot be modified.
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
