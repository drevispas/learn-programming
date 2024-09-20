package org.demo.carbe.web;

import lombok.RequiredArgsConstructor;
import org.demo.carbe.domain.Car;
import org.demo.carbe.domain.Owner;
import org.demo.carbe.service.CarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/cars")
@RestController
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<Car> listAllCars() {
        return carService.listAllCars();
    }

    @GetMapping("/owners")
    public List<Owner> listAllOwners() {
        return carService.listAllOwners();
    }
}
