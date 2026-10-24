package com.smartparking.allocation;

import com.smartparking.model.ParkingSpot;
import com.smartparking.model.SpotType;
import com.smartparking.model.VehicleType;
import com.smartparking.service.LotLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestFirstAllocatorTest {

    private final List<ParkingSpot> layout = LotLayout.uniformFloors(2, 1, 1, 1);
    private final NearestFirstAllocator allocator = new NearestFirstAllocator(layout);

    @Test
    void allocatesSpotMatchingVehicleSize() {
        assertEquals(SpotType.SMALL, allocator.allocate(VehicleType.MOTORCYCLE).orElseThrow().type());
        assertEquals(SpotType.MEDIUM, allocator.allocate(VehicleType.CAR).orElseThrow().type());
        assertEquals(SpotType.LARGE, allocator.allocate(VehicleType.BUS).orElseThrow().type());
    }

    @Test
    void picksLowestFloorFirst() {
        ParkingSpot first = allocator.allocate(VehicleType.CAR).orElseThrow();
        ParkingSpot second = allocator.allocate(VehicleType.CAR).orElseThrow();
        assertEquals(1, first.floor());
        assertEquals(2, second.floor());
    }

    @Test
    void motorcycleFallsBackToLargerSpotsWhenSmallAreTaken() {
        allocator.allocate(VehicleType.MOTORCYCLE);
        allocator.allocate(VehicleType.MOTORCYCLE);
        assertEquals(SpotType.MEDIUM, allocator.allocate(VehicleType.MOTORCYCLE).orElseThrow().type());
    }

    @Test
    void busNeverTakesSmallerSpots() {
        allocator.allocate(VehicleType.BUS);
        allocator.allocate(VehicleType.BUS);
        assertTrue(allocator.allocate(VehicleType.BUS).isEmpty());
        assertEquals(2, allocator.availability().get(SpotType.SMALL));
    }

    @Test
    void returnsEmptyWhenNothingFits() {
        allocator.allocate(VehicleType.CAR);
        allocator.allocate(VehicleType.CAR);
        allocator.allocate(VehicleType.CAR);
        allocator.allocate(VehicleType.CAR);
        assertTrue(allocator.allocate(VehicleType.CAR).isEmpty());
    }

    @Test
    void releasedSpotBecomesAvailableAgain() {
        ParkingSpot spot = allocator.allocate(VehicleType.BUS).orElseThrow();
        assertEquals(1, allocator.availability().get(SpotType.LARGE));
        allocator.release(spot);
        assertEquals(2, allocator.availability().get(SpotType.LARGE));
        assertEquals(spot, allocator.allocate(VehicleType.BUS).orElseThrow());
    }

    @Test
    void releasingFreeSpotFails() {
        ParkingSpot spot = layout.get(0);
        assertThrows(IllegalStateException.class, () -> allocator.release(spot));
    }

    @Test
    void releasingUnknownSpotFails() {
        ParkingSpot stranger = new ParkingSpot(9, 9, SpotType.SMALL);
        assertThrows(IllegalArgumentException.class, () -> allocator.release(stranger));
    }

    @Test
    void availabilityReflectsInitialCapacity() {
        assertEquals(Map.of(SpotType.SMALL, 2, SpotType.MEDIUM, 2, SpotType.LARGE, 2),
                allocator.availability());
    }
}
