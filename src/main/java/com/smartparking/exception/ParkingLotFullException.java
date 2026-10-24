package com.smartparking.exception;

import com.smartparking.model.VehicleType;

public class ParkingLotFullException extends RuntimeException {

    public ParkingLotFullException(VehicleType type) {
        super("No free spot available for " + type);
    }
}
