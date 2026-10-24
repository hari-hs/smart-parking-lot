package com.smartparking.fee;

import com.smartparking.model.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

public interface FeeCalculator {

    BigDecimal calculate(VehicleType vehicleType, Instant entryTime, Instant exitTime);
}
