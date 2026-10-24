package com.smartparking.notification;

import com.smartparking.model.SpotType;

import java.util.Map;

public interface AvailabilityListener {

    void onAvailabilityChanged(Map<SpotType, Integer> available);
}
