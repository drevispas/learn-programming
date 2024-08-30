package org.demo.dopv11.user;

public sealed interface Contact permits Email, Phone, Address {
}
