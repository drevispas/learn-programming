package org.demo.dopv11.data.item;

// Sealed interfaces do not describe behavior, but rather grouping
public sealed interface Item permits Book, Furniture {
}
