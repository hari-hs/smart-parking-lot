package com.smartparking.model;

import java.util.Comparator;

public record ParkingSpot(int floor, int number, SpotType type) implements Comparable<ParkingSpot> {

    private static final Comparator<ParkingSpot> ORDER =
            Comparator.comparingInt(ParkingSpot::floor).thenComparingInt(ParkingSpot::number);

    public String id() {
        return String.format("F%d-%s%02d", floor, type.code(), number);
    }

    @Override
    public int compareTo(ParkingSpot other) {
        return ORDER.compare(this, other);
    }
}
