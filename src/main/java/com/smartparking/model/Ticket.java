package com.smartparking.model;

import com.smartparking.exception.TicketAlreadyClosedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public final class Ticket {

    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;
    private Instant exitTime;
    private BigDecimal fee;

    public Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public synchronized void close(Instant exitTime, BigDecimal fee) {
        if (this.exitTime != null) {
            throw new TicketAlreadyClosedException(id);
        }
        this.exitTime = exitTime;
        this.fee = fee;
    }

    public String id() {
        return id;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public ParkingSpot spot() {
        return spot;
    }

    public Instant entryTime() {
        return entryTime;
    }

    public synchronized Optional<Instant> exitTime() {
        return Optional.ofNullable(exitTime);
    }

    public synchronized Optional<BigDecimal> fee() {
        return Optional.ofNullable(fee);
    }

    public synchronized boolean isClosed() {
        return exitTime != null;
    }
}
