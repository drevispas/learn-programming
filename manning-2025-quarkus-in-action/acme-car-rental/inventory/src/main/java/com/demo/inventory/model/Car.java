package com.demo.inventory.model;

public record Car(
        Long id,
        String licensePlateNumber,
        String manufacturer,
        String model
) {

    public Car(String licensePlateNumber, String manufacturer, String model) {
        this(null, licensePlateNumber, manufacturer, model);
    }

    public Car withId(Long id) {
        return new Car(id, this.licensePlateNumber, this.manufacturer, this.model);
    }
}
