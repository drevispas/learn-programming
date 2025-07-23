package com.demo.inventory.service;

import com.demo.inventory.database.CarInventory;
import com.demo.inventory.model.Car;
import com.demo.inventory.model.CarResponse;
import com.demo.inventory.model.InsertCarRequest;
import com.demo.inventory.model.InventoryService;
import com.demo.inventory.model.RemoveCarRequest;
import com.demo.inventory.repository.CarRepository;
import io.quarkus.grpc.GrpcService;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;

@GrpcService
public class GrpcInventoryService implements InventoryService {

    @Inject
    CarInventory carInventory;
    @Inject
    CarRepository carRepository;

    @Override
    @Blocking
    @Transactional
    public Uni<CarResponse> add(InsertCarRequest request) {
        Car car = new Car(null, request.getLicensePlateNumber(), request.getManufacturer(), request.getModel());
        carRepository.persist(car);
        return Uni.createFrom().item(CarResponse.newBuilder()
                .setLicensePlateNumber(car.getLicensePlateNumber())
                .setManufacturer(car.getManufacturer())
                .setModel(car.getModel())
                .setId(car.getId())
                .build()
        );
    }

    @Override
    @Blocking
    public Multi<CarResponse> addMultiple(Multi<InsertCarRequest> requests) {
        return requests.map(request ->
                new Car(null, request.getLicensePlateNumber(), request.getManufacturer(), request.getModel())
        ).onItem().invoke(car ->
            QuarkusTransaction.requiringNew().run(() -> {
                carRepository.persist(car);
                Log.info("Persisted car: " + car);
            })
        ).map(car -> CarResponse.newBuilder()
                .setLicensePlateNumber(car.getLicensePlateNumber())
                .setManufacturer(car.getManufacturer())
                .setModel(car.getModel())
                .setId(car.getId())
                .build()
        );
    }

    @Override
    @Blocking
    @Transactional
    public Uni<CarResponse> remove(RemoveCarRequest request) {
        Optional<Car> car = carRepository.findByLicensePlateNumberOptional(request.getLicensePlateNumber());
        if (car.isPresent()) {
            Car removedCar = car.get();
            carRepository.delete(removedCar);
            return Uni.createFrom().item(CarResponse.newBuilder()
                    .setLicensePlateNumber(removedCar.getLicensePlateNumber())
                    .setManufacturer(removedCar.getManufacturer())
                    .setModel(removedCar.getModel())
                    .setId(removedCar.getId())
                    .build()
            );
        } else {
            return Uni.createFrom().failure(new RuntimeException("Car not found"));
        }
    }
}
