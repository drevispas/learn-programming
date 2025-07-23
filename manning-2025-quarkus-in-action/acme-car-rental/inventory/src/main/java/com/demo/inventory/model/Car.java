package com.demo.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

//public record Car(
//        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//        Long id,
//        String licensePlateNumber,
//        String manufacturer,
//        String model
//) {
//
//    public Car() {
//        this(null, null, null, null);
//    }
//
//    public Car(String licensePlateNumber, String manufacturer, String model) {
//        this(null, licensePlateNumber, manufacturer, model);
//    }
//
//    public Car withId(Long id) {
//        return new Car(id, this.licensePlateNumber, this.manufacturer, this.model);
//    }
//}

@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String licensePlateNumber;
    private String manufacturer;
    private String model;

    public Car() {
    }

    public Car(Long id, String licensePlateNumber, String manufacturer, String model) {
        this.id = id;
        this.licensePlateNumber = licensePlateNumber;
        this.manufacturer = manufacturer;
        this.model = model;
    }

    public Car withId(Long id) {
        return new Car(id, this.licensePlateNumber, this.manufacturer, this.model);
    }

    public Long getId() {
        return id;
    }

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLicensePlateNumber(String licensePlateNumber) {
        this.licensePlateNumber = licensePlateNumber;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
