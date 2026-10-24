package com.smartparking.allocation;

import com.smartparking.model.ParkingSpot;
import com.smartparking.model.SpotType;
import com.smartparking.model.VehicleType;

import java.util.Map;
import java.util.Optional;

public interface SpotAllocator {

    Optional<ParkingSpot> allocate(VehicleType vehicleType);

    void release(ParkingSpot spot);

    Map<SpotType, Integer> availability();
}
