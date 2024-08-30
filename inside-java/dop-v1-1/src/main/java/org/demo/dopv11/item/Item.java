package org.demo.dopv11.item;

// Sealed interfaces do not describe behavior, but rather grouping
public sealed interface Item permits Book, Furniture {
}
