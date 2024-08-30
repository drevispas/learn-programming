package org.demo.dopv11.user.data;

public record Address(String street, String city, String zipCode) implements Contact {
}
