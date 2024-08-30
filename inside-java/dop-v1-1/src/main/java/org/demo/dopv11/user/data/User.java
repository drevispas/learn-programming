package org.demo.dopv11.user.data;

//public record User(Contact primaryContact, List<Contact> secondaryContacts) {
//}

sealed interface User permits RegisteredUser, UnregisteredUser {
}
