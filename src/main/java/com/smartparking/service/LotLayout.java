package com.smartparking.service;

import com.smartparking.model.ParkingSpot;
import com.smartparking.model.SpotType;

import java.util.ArrayList;
import java.util.List;

public final class LotLayout {

    private LotLayout() {
    }

    public static List<ParkingSpot> uniformFloors(int floors, int small, int medium, int large) {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int floor = 1; floor <= floors; floor++) {
            int number = 1;
            for (int i = 0; i < small; i++) {
                spots.add(new ParkingSpot(floor, number++, SpotType.SMALL));
            }
            for (int i = 0; i < medium; i++) {
                spots.add(new ParkingSpot(floor, number++, SpotType.MEDIUM));
            }
            for (int i = 0; i < large; i++) {
                spots.add(new ParkingSpot(floor, number++, SpotType.LARGE));
            }
        }
        return spots;
    }
}
