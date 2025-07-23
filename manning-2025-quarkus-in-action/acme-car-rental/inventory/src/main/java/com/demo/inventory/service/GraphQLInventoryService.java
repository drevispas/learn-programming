package com.demo.inventory.service;

import com.demo.inventory.database.CarInventory;
import com.demo.inventory.model.Car;
import com.demo.inventory.repository.CarRepository;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class GraphQLInventoryService {

    @Inject
    CarRepository carRepository;

    @Query
    public List<Car> cars() {
        return carRepository.listAll();
    }

    @Transactional
    @Mutation
    public Car register(Car car) {
        carRepository.persist(car);
        Log.info("Persisted car: " + car);
        return car;
    }

    @Transactional
    @Mutation
    public boolean remove(String licensePlateNumber) {
        Optional<Car> carOptional = carRepository.findByLicensePlateNumberOptional(licensePlateNumber);
        if (carOptional.isPresent()) {
            carRepository.delete(carOptional.get());
            return true;
        }
        return false;
    }
}
