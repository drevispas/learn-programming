package com.demo.inventory.model;

public record Car(
        Long id,
        String licensePlateNumber,
        String manufacturer,
        String model
) {
}
