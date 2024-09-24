package org.demo.carbe.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private String color;
    private String registerNumber;
    private int manufacturingYear;
    private int price;

    @ManyToMany(mappedBy = "cars")
    private Set<Owner> owners = new HashSet<>();
}
