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
        return (List<Car>) carRepository.findAll();
    }

    public List<Owner> listAllOwners() {
        return (List<Owner>) ownerRepository.findAll();
    }

    public Owner getOwnerById(long id) {
        return ownerRepository.findById(id).orElse(null);
    }

    public Car getCarById(long id) {
        return carRepository.findById(id).orElse(null);
    }
}
