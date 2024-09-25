package org.demo.carbe.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties({"cars"})
@Data
@NoArgsConstructor
@Entity
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String firstName;
    private String lastName;

//    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
//    @JoinTable(name = "owner_car",
//            joinColumns = @JoinColumn(name = "owner_id"),
//            inverseJoinColumns = @JoinColumn(name = "car_id"))
//    private Set<Car> cars = new HashSet<>();
//
//    public void addCar(Car car) {
//        cars.add(car);
//        car.getOwners().add(this);
//    }
//
//    public void removeCar(Car car) {
//        cars.remove(car);
//        car.getOwners().remove(this);
//    }

//    @JsonIgnore
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Car> cars = new ArrayList<>();

    public void addCar(Car car) {
        cars.add(car);
        car.setOwner(this);
    }

    public void removeCar(Car car) {
        cars.remove(car);
        car.setOwner(null);
    }
}
