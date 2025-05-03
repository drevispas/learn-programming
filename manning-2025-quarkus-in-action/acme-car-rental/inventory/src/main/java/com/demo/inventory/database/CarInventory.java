package com.demo.inventory.database;

import com.demo.inventory.model.Car;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class CarInventory {

    private List<Car> cars;

    public static final AtomicLong counter = new AtomicLong(0);

    @PostConstruct
    void init() {
        cars = new CopyOnWriteArrayList<>();
        cars.add(new Car(counter.incrementAndGet(), "ABC123", "Toyota", "Camry"));
        cars.add(new Car(counter.incrementAndGet(), "XYZ789", "Honda", "Civic"));
        cars.add(new Car(counter.incrementAndGet(), "LMN456", "Ford", "Mustang"));
        cars.add(new Car(counter.incrementAndGet(), "DEF321", "Chevrolet", "Impala"));
        cars.add(new Car(counter.incrementAndGet(), "GHI654", "Nissan", "Altima"));
    }

    public List<Car> listCars() {
        return cars;
    }

    public Car registerCar(Car car) {
        car = car.withId(counter.incrementAndGet());
        cars.add(car);
        return car;
    }

    public boolean removeCar(String licensePlateNumber) {
        Optional<Car> toBeRemoved = listCars().stream()
                .filter(car -> car.licensePlateNumber().equals(licensePlateNumber))
                .findAny();
        return toBeRemoved.map(cars::remove).orElse(false);
    }
}
