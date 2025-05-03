package com.demo.reservation.inventory;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class InMemoryInventoryClient implements InventoryClient {

    private static final List<Car> ALL_CARS = List.of(
            new Car(1L, "ABC123", "Toyota", "Camry"),
            new Car(2L, "XYZ789", "Honda", "Civic"),
            new Car(3L, "LMN456", "Ford", "Mustang"),
            new Car(4L, "DEF321", "Chevrolet", "Impala"),
            new Car(5L, "GHI654", "Nissan", "Altima")
    );

    @Override
    public List<Car> listAllCars() {
        return ALL_CARS;
    }
}
