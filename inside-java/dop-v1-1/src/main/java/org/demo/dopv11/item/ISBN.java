package org.demo.dopv11.item;

public record ISBN(String value) {

    public ISBN(ISBN isbn) {
        this(isbn.value());
    }
}
