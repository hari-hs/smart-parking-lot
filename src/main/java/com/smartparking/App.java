package com.smartparking;

import com.smartparking.allocation.NearestFirstAllocator;
import com.smartparking.fee.HourlyFeeCalculator;
import com.smartparking.model.Ticket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.notification.DisplayBoard;
import com.smartparking.repository.InMemoryTicketRepository;
import com.smartparking.service.LotLayout;
import com.smartparking.service.ParkingLotService;
import com.smartparking.support.ManualClock;

import java.time.Duration;
import java.time.Instant;

public class App {

    public static void main(String[] args) {
        ManualClock clock = new ManualClock(Instant.parse("2026-01-10T09:00:00Z"));
        ParkingLotService lot = new ParkingLotService(
                new NearestFirstAllocator(LotLayout.uniformFloors(2, 2, 3, 1)),
                HourlyFeeCalculator.withDefaultRates(),
                new InMemoryTicketRepository(),
                clock);
        lot.addListener(new DisplayBoard(System.out));

        Ticket bike = lot.checkIn(new Vehicle("TN01AB1234", VehicleType.MOTORCYCLE));
        Ticket car = lot.checkIn(new Vehicle("TN09CD5678", VehicleType.CAR));
        Ticket bus = lot.checkIn(new Vehicle("TN22EF9012", VehicleType.BUS));
        print("Parked", bike);
        print("Parked", car);
        print("Parked", bus);

        clock.advance(Duration.ofMinutes(10));
        print("Left after 10 min", lot.checkOut(bike.id()));

        clock.advance(Duration.ofHours(2).plusMinutes(5));
        print("Left after 2h 15m", lot.checkOut(car.id()));

        clock.advance(Duration.ofHours(30));
        print("Left after 32h 15m", lot.checkOut(bus.id()));
    }

    private static void print(String action, Ticket ticket) {
        String fee = ticket.fee().map(f -> ", fee " + f).orElse("");
        System.out.printf("%s: %s %s at %s%s%n", action, ticket.vehicle().type(),
                ticket.vehicle().licensePlate(), ticket.spot().id(), fee);
    }
}
