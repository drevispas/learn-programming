package org.demo.dopv11.item.operation;

import org.demo.dopv11.item.data.Book;
import org.demo.dopv11.item.data.Furniture;
import org.demo.dopv11.item.data.ISBN;
import org.demo.dopv11.item.data.Item;
import org.demo.dopv11.user.data.RegisteredUser;

public class Reservation {

    public static boolean reserve(Item item, RegisteredUser user) {
        return switch (item) {
            case Book(String title, ISBN(String isbn), _) when isbn.startsWith("X") -> {
                System.out.println("Reserving book of X " + title);
                yield true;
            }
            case Book(String title, _, _) -> {
                System.out.println("Reserving book " + title);
                yield true;
            }
            case Furniture _ -> {
                System.out.println("Reserving furniture is not supported yet!");
                yield false;
            }
        };
    }
}
