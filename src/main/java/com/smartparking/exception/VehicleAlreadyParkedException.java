package com.smartparking.exception;

public class VehicleAlreadyParkedException extends RuntimeException {

    public VehicleAlreadyParkedException(String licensePlate) {
        super("Vehicle is already parked: " + licensePlate);
    }
}
