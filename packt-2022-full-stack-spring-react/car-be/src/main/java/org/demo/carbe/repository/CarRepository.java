package org.demo.carbe.repository;

import org.demo.carbe.domain.Car;
import org.springframework.data.repository.CrudRepository;

public interface CarRepository extends CrudRepository<Car, Long> {
}
