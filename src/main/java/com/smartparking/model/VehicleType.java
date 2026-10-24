package com.smartparking.model;

import java.util.List;

public enum VehicleType {
    MOTORCYCLE(SpotType.SMALL, SpotType.MEDIUM, SpotType.LARGE),
    CAR(SpotType.MEDIUM, SpotType.LARGE),
    BUS(SpotType.LARGE);

    private final List<SpotType> compatibleSpots;

    VehicleType(SpotType... spots) {
        this.compatibleSpots = List.of(spots);
    }

    public List<SpotType> compatibleSpots() {
        return compatibleSpots;
    }
}
