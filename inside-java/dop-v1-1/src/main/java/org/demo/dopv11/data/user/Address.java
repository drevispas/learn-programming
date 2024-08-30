package org.demo.dopv11.data.user;

public record Address(String street, String city, String zipCode) implements Contact {
}
