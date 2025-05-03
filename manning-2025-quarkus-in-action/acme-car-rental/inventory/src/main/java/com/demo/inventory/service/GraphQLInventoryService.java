package com.demo.inventory.service;

import com.demo.inventory.database.CarInventory;
import com.demo.inventory.model.Car;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class GraphQLInventoryService {

    @Inject
    CarInventory carInventory;

    @Query
    public List<Car> cars() {
        return carInventory.listCars();
    }

    @Mutation
    public Car register(Car car) {
        return carInventory.registerCar(car);
    }

    @Mutation
    public boolean remove(String licensePlateNumber) {
        return carInventory.removeCar(licensePlateNumber);
    }
}
