package org.demo.dopv11.user;

import java.util.List;

public record User(Contact primaryContact, List<Contact> secondaryContacts) {
}
