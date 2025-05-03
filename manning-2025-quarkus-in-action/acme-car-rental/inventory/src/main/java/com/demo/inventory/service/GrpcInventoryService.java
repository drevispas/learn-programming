package com.demo.inventory.service;

import com.demo.inventory.database.CarInventory;
import com.demo.inventory.model.Car;
import com.demo.inventory.model.CarResponse;
import com.demo.inventory.model.InsertCarRequest;
import com.demo.inventory.model.InventoryService;
import com.demo.inventory.model.RemoveCarRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.Optional;

@GrpcService
public class GrpcInventoryService implements InventoryService {

    @Inject
    CarInventory carInventory;

    @Override
    public Uni<CarResponse> add(InsertCarRequest request) {
        Car car = carInventory.registerCar(new Car(request.getLicensePlateNumber(), request.getManufacturer(), request.getModel()));
        return Uni.createFrom().item(CarResponse.newBuilder()
                .setLicensePlateNumber(car.licensePlateNumber())
                .setManufacturer(car.manufacturer())
                .setModel(car.model())
                .setId(car.id())
                .build()
        );
    }

//    @Override
//    public Multi<CarResponse> add(Multi<InsertCarRequest> requests) {
//        return requests.map(request ->
//                new Car(request.getLicensePlateNumber(), request.getManufacturer(), request.getModel())
//        ).onItem().invoke(car ->
//                carInventory.registerCar(car)
//        ).map(car -> CarResponse.newBuilder()
//                .setLicensePlateNumber(car.licensePlateNumber())
//                .setManufacturer(car.manufacturer())
//                .setModel(car.model())
//                .setId(car.id())
//                .build()
//        );
//    }

    @Override
    public Uni<CarResponse> remove(RemoveCarRequest request) {
        Optional<Car> car = carInventory.listCars().stream()
                .filter(c -> c.licensePlateNumber().equals(request.getLicensePlateNumber()))
                .findFirst();
        if (car.isPresent()) {
            carInventory.removeCar(request.getLicensePlateNumber());
            return Uni.createFrom().item(CarResponse.newBuilder()
                    .setLicensePlateNumber(car.get().licensePlateNumber())
                    .setManufacturer(car.get().manufacturer())
                    .setModel(car.get().model())
                    .setId(car.get().id())
                    .build()
            );
        } else {
            return Uni.createFrom().failure(new RuntimeException("Car not found"));
        }
    }
}
