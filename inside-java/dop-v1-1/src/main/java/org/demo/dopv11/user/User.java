package org.demo.dopv11.user;

import java.util.List;

//public record User(Contact primaryContact, List<Contact> secondaryContacts) {
//}

sealed interface User {}

record UnregisteredUser(String name) implements User {
}

record RegisteredUser(String name, Email email) {

    RegisteredUser {
        if (email == null || email.address() == null || email.address().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
    }
}
