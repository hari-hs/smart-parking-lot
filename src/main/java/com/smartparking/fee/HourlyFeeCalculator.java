package com.smartparking.fee;

import com.smartparking.model.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public class HourlyFeeCalculator implements FeeCalculator {

    public record Rate(BigDecimal hourly, BigDecimal dailyCap) {

        public Rate {
            if (hourly.signum() < 0 || dailyCap.signum() < 0) {
                throw new IllegalArgumentException("rates must not be negative");
            }
        }
    }

    private static final long SECONDS_PER_HOUR = 3600;
    private static final long HOURS_PER_DAY = 24;

    private final Map<VehicleType, Rate> rates;
    private final Duration gracePeriod;

    public HourlyFeeCalculator(Map<VehicleType, Rate> rates, Duration gracePeriod) {
        for (VehicleType type : VehicleType.values()) {
            if (!rates.containsKey(type)) {
                throw new IllegalArgumentException("Missing rate for " + type);
            }
        }
        this.rates = new EnumMap<>(rates);
        this.gracePeriod = gracePeriod;
    }

    public static HourlyFeeCalculator withDefaultRates() {
        Map<VehicleType, Rate> rates = new EnumMap<>(VehicleType.class);
        rates.put(VehicleType.MOTORCYCLE, new Rate(BigDecimal.valueOf(10), BigDecimal.valueOf(80)));
        rates.put(VehicleType.CAR, new Rate(BigDecimal.valueOf(20), BigDecimal.valueOf(150)));
        rates.put(VehicleType.BUS, new Rate(BigDecimal.valueOf(50), BigDecimal.valueOf(400)));
        return new HourlyFeeCalculator(rates, Duration.ofMinutes(15));
    }

    @Override
    public BigDecimal calculate(VehicleType vehicleType, Instant entryTime, Instant exitTime) {
        Duration stay = Duration.between(entryTime, exitTime);
        if (stay.isNegative()) {
            throw new IllegalArgumentException("Exit time is before entry time");
        }
        if (stay.compareTo(gracePeriod) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        Rate rate = rates.get(vehicleType);
        long stayedSeconds = stay.getSeconds() + (stay.getNano() > 0 ? 1 : 0);
        long billedHours = (stayedSeconds + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR;
        long fullDays = billedHours / HOURS_PER_DAY;
        long remainingHours = billedHours % HOURS_PER_DAY;

        BigDecimal partialDay = rate.hourly().multiply(BigDecimal.valueOf(remainingHours)).min(rate.dailyCap());
        BigDecimal total = rate.dailyCap().multiply(BigDecimal.valueOf(fullDays)).add(partialDay);
        return total.setScale(2);
    }
}
