package org.demo.dopv11.data.item;

public record ISBN(String value) {

    public ISBN(ISBN isbn) {
        this(isbn.value());
    }
}
