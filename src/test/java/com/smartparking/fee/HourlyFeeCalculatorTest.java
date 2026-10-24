package com.smartparking.fee;

import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HourlyFeeCalculatorTest {

    private static final Instant ENTRY = Instant.parse("2026-01-10T09:00:00Z");

    private final HourlyFeeCalculator calculator = HourlyFeeCalculator.withDefaultRates();

    private BigDecimal fee(VehicleType type, Duration stay) {
        return calculator.calculate(type, ENTRY, ENTRY.plus(stay));
    }

    private static BigDecimal money(int amount) {
        return BigDecimal.valueOf(amount).setScale(2);
    }

    @Test
    void stayWithinGracePeriodIsFree() {
        assertEquals(money(0), fee(VehicleType.CAR, Duration.ZERO));
        assertEquals(money(0), fee(VehicleType.CAR, Duration.ofMinutes(15)));
    }

    @Test
    void stayJustPastGraceBillsOneHour() {
        assertEquals(money(20), fee(VehicleType.CAR, Duration.ofMinutes(16)));
    }

    @Test
    void partialHoursAreRoundedUp() {
        assertEquals(money(20), fee(VehicleType.CAR, Duration.ofHours(1)));
        assertEquals(money(40), fee(VehicleType.CAR, Duration.ofHours(1).plusSeconds(1)));
        assertEquals(money(60), fee(VehicleType.CAR, Duration.ofHours(2).plusMinutes(15)));
    }

    @Test
    void rateDependsOnVehicleType() {
        Duration stay = Duration.ofHours(2);
        assertEquals(money(20), fee(VehicleType.MOTORCYCLE, stay));
        assertEquals(money(40), fee(VehicleType.CAR, stay));
        assertEquals(money(100), fee(VehicleType.BUS, stay));
    }

    @Test
    void dailyCapLimitsPartialDay() {
        assertEquals(money(150), fee(VehicleType.CAR, Duration.ofHours(10)));
        assertEquals(money(150), fee(VehicleType.CAR, Duration.ofHours(24)));
    }

    @Test
    void multiDayStaysChargeCapPerDayPlusRemainder() {
        assertEquals(money(300), fee(VehicleType.CAR, Duration.ofHours(48)));
        assertEquals(money(320), fee(VehicleType.CAR, Duration.ofHours(49)));
        assertEquals(money(800), fee(VehicleType.BUS, Duration.ofHours(32).plusMinutes(15)));
    }

    @Test
    void exitBeforeEntryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(VehicleType.CAR, ENTRY, ENTRY.minusSeconds(1)));
    }

    @Test
    void incompleteRateTableIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new HourlyFeeCalculator(Map.of(), Duration.ZERO));
    }
}
