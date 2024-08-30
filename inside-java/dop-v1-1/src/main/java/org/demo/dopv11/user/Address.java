package org.demo.dopv11.user;

public record Address(String street, String city, String zipCode) implements Contact {
}
