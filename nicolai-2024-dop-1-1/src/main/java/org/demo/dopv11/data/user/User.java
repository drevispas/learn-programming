package org.demo.dopv11.data.user;

//public record User(Contact primaryContact, List<Contact> secondaryContacts) {
//}

sealed interface User permits RegisteredUser, UnregisteredUser {
}
