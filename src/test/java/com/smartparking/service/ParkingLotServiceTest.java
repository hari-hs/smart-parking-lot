package com.smartparking.service;

import com.smartparking.allocation.NearestFirstAllocator;
import com.smartparking.exception.ParkingLotFullException;
import com.smartparking.exception.TicketAlreadyClosedException;
import com.smartparking.exception.TicketNotFoundException;
import com.smartparking.exception.VehicleAlreadyParkedException;
import com.smartparking.fee.HourlyFeeCalculator;
import com.smartparking.model.SpotType;
import com.smartparking.model.Ticket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.InMemoryTicketRepository;
import com.smartparking.support.ManualClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotServiceTest {

    private ManualClock clock;
    private ParkingLotService service;

    @BeforeEach
    void setUp() {
        clock = new ManualClock(Instant.parse("2026-01-10T09:00:00Z"));
        service = new ParkingLotService(
                new NearestFirstAllocator(LotLayout.uniformFloors(1, 1, 1, 1)),
                HourlyFeeCalculator.withDefaultRates(),
                new InMemoryTicketRepository(),
                clock);
    }

    private static Vehicle car(String plate) {
        return new Vehicle(plate, VehicleType.CAR);
    }

    @Test
    void checkInRecordsEntryTimeAndSpot() {
        Ticket ticket = service.checkIn(car("TN01A0001"));
        assertEquals(clock.instant(), ticket.entryTime());
        assertEquals(SpotType.MEDIUM, ticket.spot().type());
        assertFalse(ticket.isClosed());
    }

    @Test
    void checkOutRecordsExitTimeAndFee() {
        Ticket ticket = service.checkIn(car("TN01A0001"));
        clock.advance(Duration.ofHours(3));
        Ticket closed = service.checkOut(ticket.id());

        assertEquals(clock.instant(), closed.exitTime().orElseThrow());
        assertEquals(new BigDecimal("60.00"), closed.fee().orElseThrow());
    }

    @Test
    void availabilityDropsOnEntryAndRecoversOnExit() {
        Ticket ticket = service.checkIn(car("TN01A0001"));
        assertEquals(0, service.availability().get(SpotType.MEDIUM));
        service.checkOut(ticket.id());
        assertEquals(1, service.availability().get(SpotType.MEDIUM));
    }

    @Test
    void listenersReceiveLatestAvailability() {
        List<Map<SpotType, Integer>> updates = new ArrayList<>();
        service.addListener(updates::add);

        Ticket ticket = service.checkIn(car("TN01A0001"));
        service.checkOut(ticket.id());

        assertEquals(2, updates.size());
        assertEquals(0, updates.get(0).get(SpotType.MEDIUM));
        assertEquals(1, updates.get(1).get(SpotType.MEDIUM));
    }

    @Test
    void failingListenerDoesNotBreakCheckIn() {
        service.addListener(snapshot -> {
            throw new IllegalStateException("display offline");
        });
        Ticket ticket = service.checkIn(car("TN01A0001"));
        assertFalse(ticket.isClosed());
    }

    @Test
    void sameVehicleCannotCheckInTwice() {
        service.checkIn(car("TN01A0001"));
        assertThrows(VehicleAlreadyParkedException.class, () -> service.checkIn(car(" tn01a0001 ")));
    }

    @Test
    void vehicleCanReturnAfterCheckingOut() {
        Ticket first = service.checkIn(car("TN01A0001"));
        service.checkOut(first.id());
        Ticket second = service.checkIn(car("TN01A0001"));
        assertFalse(second.isClosed());
    }

    @Test
    void fullLotRejectsEntryAndDoesNotBlockPlate() {
        service.checkIn(car("TN01A0001"));
        service.checkIn(car("TN01A0002"));
        assertThrows(ParkingLotFullException.class, () -> service.checkIn(car("TN01A0003")));
        assertThrows(ParkingLotFullException.class, () -> service.checkIn(car("TN01A0003")));
    }

    @Test
    void unknownTicketIsRejected() {
        assertThrows(TicketNotFoundException.class, () -> service.checkOut("missing"));
    }

    @Test
    void ticketCannotBeCheckedOutTwice() {
        Ticket ticket = service.checkIn(car("TN01A0001"));
        service.checkOut(ticket.id());
        assertThrows(TicketAlreadyClosedException.class, () -> service.checkOut(ticket.id()));
        assertEquals(1, service.availability().get(SpotType.MEDIUM));
    }

    @Test
    void licensePlateIsNormalised() {
        assertEquals("TN01A0001", car("  tn01a0001 ").licensePlate());
        assertTrue(car("tn01a0001").equals(car("TN01A0001")));
    }
}
