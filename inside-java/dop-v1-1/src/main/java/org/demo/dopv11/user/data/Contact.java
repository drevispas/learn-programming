package org.demo.dopv11.user.data;

public sealed interface Contact permits Email, Phone, Address {
}
