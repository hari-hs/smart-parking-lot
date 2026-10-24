package com.smartparking.model;

import java.util.Objects;

public record Vehicle(String licensePlate, VehicleType type) {

    public Vehicle {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(licensePlate, "licensePlate");
        licensePlate = licensePlate.trim().toUpperCase();
        if (licensePlate.isEmpty()) {
            throw new IllegalArgumentException("licensePlate must not be blank");
        }
    }
}
