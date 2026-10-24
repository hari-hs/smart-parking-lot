package com.smartparking.service;

import com.smartparking.allocation.SpotAllocator;
import com.smartparking.exception.ParkingLotFullException;
import com.smartparking.exception.TicketNotFoundException;
import com.smartparking.exception.VehicleAlreadyParkedException;
import com.smartparking.fee.FeeCalculator;
import com.smartparking.model.ParkingSpot;
import com.smartparking.model.SpotType;
import com.smartparking.model.Ticket;
import com.smartparking.model.Vehicle;
import com.smartparking.notification.AvailabilityListener;
import com.smartparking.repository.TicketRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParkingLotService {

    private static final Logger LOG = Logger.getLogger(ParkingLotService.class.getName());

    private final SpotAllocator allocator;
    private final FeeCalculator feeCalculator;
    private final TicketRepository tickets;
    private final Clock clock;
    private final Set<String> parkedPlates = ConcurrentHashMap.newKeySet();
    private final List<AvailabilityListener> listeners = new CopyOnWriteArrayList<>();
    private final Object notifyLock = new Object();

    public ParkingLotService(SpotAllocator allocator, FeeCalculator feeCalculator,
                             TicketRepository tickets, Clock clock) {
        this.allocator = allocator;
        this.feeCalculator = feeCalculator;
        this.tickets = tickets;
        this.clock = clock;
    }

    public void addListener(AvailabilityListener listener) {
        listeners.add(listener);
    }

    public Map<SpotType, Integer> availability() {
        return allocator.availability();
    }

    public Ticket checkIn(Vehicle vehicle) {
        if (!parkedPlates.add(vehicle.licensePlate())) {
            throw new VehicleAlreadyParkedException(vehicle.licensePlate());
        }

        ParkingSpot spot = null;
        Ticket ticket;
        try {
            spot = allocator.allocate(vehicle.type())
                    .orElseThrow(() -> new ParkingLotFullException(vehicle.type()));
            ticket = new Ticket(UUID.randomUUID().toString(), vehicle, spot, clock.instant());
            tickets.save(ticket);
        } catch (RuntimeException e) {
            if (spot != null) {
                allocator.release(spot);
            }
            parkedPlates.remove(vehicle.licensePlate());
            throw e;
        }

        publishAvailability();
        return ticket;
    }

    public Ticket checkOut(String ticketId) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Instant exitTime = clock.instant();
        BigDecimal fee = feeCalculator.calculate(ticket.vehicle().type(), ticket.entryTime(), exitTime);
        ticket.close(exitTime, fee);

        allocator.release(ticket.spot());
        parkedPlates.remove(ticket.vehicle().licensePlate());
        tickets.save(ticket);

        publishAvailability();
        return ticket;
    }

    private void publishAvailability() {
        synchronized (notifyLock) {
            Map<SpotType, Integer> snapshot = allocator.availability();
            for (AvailabilityListener listener : listeners) {
                try {
                    listener.onAvailabilityChanged(snapshot);
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "Availability listener failed", e);
                }
            }
        }
    }
}
