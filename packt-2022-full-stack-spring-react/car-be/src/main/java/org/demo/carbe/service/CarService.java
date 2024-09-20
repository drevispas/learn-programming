package org.demo.carbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.carbe.domain.Car;
import org.demo.carbe.domain.Owner;
import org.demo.carbe.repository.CarRepository;
import org.demo.carbe.repository.OwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CarService {

    private final OwnerRepository ownerRepository;
    private final CarRepository carRepository;

    private List<Car> cars;
    private List<Owner> owners;

    public List<Car> listAllCars() {
        if (cars == null) {
//            loadData();
        }
        return (List<Car>) carRepository.findAll();
    }

    public List<Owner> listAllOwners() {
        if (owners == null) {
//            loadData();
        }
        return (List<Owner>) ownerRepository.findAll();
    }

    public Owner getOwnerById(long id) {
        return ownerRepository.findById(id).orElse(null);
    }

    public Car getCarById(long id) {
        return carRepository.findById(id).orElse(null);
    }

    private void loadData() {
        log.info("Loading data");
        var owner1 = new Owner(1L, "John", "Doe", null);
        var owner2 = new Owner(2L, "Mary", "Jane", null);
        ownerRepository.saveAll(List.of(owner1, owner2));
        var car1 = new Car(11L, "Toyota", "Camry", "Black", "DL-001", 2020, 35000, owner1);
        var car2 = new Car(12L, "Honda", "Civic", "White", "DL-002", 2021, 30000, owner1);
        var car3 = new Car(21L, "Ford", "Fusion", "Silver", "DL-003", 2019, 32000, owner2);
        var car4 = new Car(22L, "Chevrolet", "Malibu", "Red", "DL-004", 2020, 33000, owner2);
        carRepository.saveAll(List.of(car1, car2, car3, car4));
    }
}
