package com.demo.inventory.repository;

import com.demo.inventory.model.Car;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class CarRepository implements PanacheRepository<Car> {

    public Optional<Car> findByLicensePlateNumberOptional(String licensePlate) {
        return find("licensePlateNumber", licensePlate).firstResultOptional();
    }
}
