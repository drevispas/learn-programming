package com.demo.users.model;

public record Car(
        Long id,
        String licensePlateNumber,
        String manufacturer,
        String model
) {

    // empty constructor for deserialization
    public Car() {
        this(null, null, null, null);
    }
}
