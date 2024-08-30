package org.demo.dopv11.data.user;

public sealed interface Contact permits Email, Phone, Address {
}
