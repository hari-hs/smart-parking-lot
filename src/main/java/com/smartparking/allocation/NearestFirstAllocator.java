package com.smartparking.allocation;

import com.smartparking.model.ParkingSpot;
import com.smartparking.model.SpotType;
import com.smartparking.model.VehicleType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

public class NearestFirstAllocator implements SpotAllocator {

    private final Set<ParkingSpot> knownSpots;
    private final Map<SpotType, TreeSet<ParkingSpot>> freeSpots = new EnumMap<>(SpotType.class);
    private final ReentrantLock lock = new ReentrantLock();

    public NearestFirstAllocator(Collection<ParkingSpot> spots) {
        this.knownSpots = new HashSet<>(spots);
        for (SpotType type : SpotType.values()) {
            freeSpots.put(type, new TreeSet<>());
        }
        for (ParkingSpot spot : knownSpots) {
            freeSpots.get(spot.type()).add(spot);
        }
    }

    @Override
    public Optional<ParkingSpot> allocate(VehicleType vehicleType) {
        lock.lock();
        try {
            for (SpotType spotType : vehicleType.compatibleSpots()) {
                TreeSet<ParkingSpot> candidates = freeSpots.get(spotType);
                if (!candidates.isEmpty()) {
                    return Optional.of(candidates.pollFirst());
                }
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void release(ParkingSpot spot) {
        if (!knownSpots.contains(spot)) {
            throw new IllegalArgumentException("Unknown spot: " + spot.id());
        }
        lock.lock();
        try {
            if (!freeSpots.get(spot.type()).add(spot)) {
                throw new IllegalStateException("Spot is already free: " + spot.id());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<SpotType, Integer> availability() {
        Map<SpotType, Integer> snapshot = new EnumMap<>(SpotType.class);
        lock.lock();
        try {
            freeSpots.forEach((type, spots) -> snapshot.put(type, spots.size()));
        } finally {
            lock.unlock();
        }
        return snapshot;
    }
}
