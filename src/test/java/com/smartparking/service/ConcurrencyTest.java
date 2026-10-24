package com.smartparking.service;

import com.smartparking.allocation.NearestFirstAllocator;
import com.smartparking.exception.ParkingLotFullException;
import com.smartparking.exception.TicketAlreadyClosedException;
import com.smartparking.exception.VehicleAlreadyParkedException;
import com.smartparking.fee.HourlyFeeCalculator;
import com.smartparking.model.SpotType;
import com.smartparking.model.Ticket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.InMemoryTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(60)
class ConcurrencyTest {

    private static final int MEDIUM_SPOTS = 10;

    private ParkingLotService newService() {
        return new ParkingLotService(
                new NearestFirstAllocator(LotLayout.uniformFloors(1, 0, MEDIUM_SPOTS, 0)),
                HourlyFeeCalculator.withDefaultRates(),
                new InMemoryTicketRepository(),
                Clock.systemUTC());
    }

    private static <T> List<Future<T>> runTogether(int threads, IntFunction<Callable<T>> job)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<T> task = job.apply(i);
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        return futures;
    }

    @Test
    void simultaneousEntriesNeverExceedCapacityOrShareSpots() throws Exception {
        ParkingLotService service = newService();
        int threads = 60;

        List<Future<Ticket>> results = runTogether(threads, i -> () -> {
            try {
                return service.checkIn(new Vehicle("CAR-" + i, VehicleType.CAR));
            } catch (ParkingLotFullException e) {
                return null;
            }
        });

        Set<String> spotIds = ConcurrentHashMap.newKeySet();
        int parked = 0;
        for (Future<Ticket> result : results) {
            Ticket ticket = result.get();
            if (ticket != null) {
                parked++;
                spotIds.add(ticket.spot().id());
            }
        }

        assertEquals(MEDIUM_SPOTS, parked);
        assertEquals(MEDIUM_SPOTS, spotIds.size());
        assertEquals(0, service.availability().get(SpotType.MEDIUM));
    }

    @Test
    void sameVehicleOnlyParksOnceWhenEnteringConcurrently() throws Exception {
        ParkingLotService service = newService();
        AtomicInteger rejected = new AtomicInteger();

        List<Future<Ticket>> results = runTogether(20, i -> () -> {
            try {
                return service.checkIn(new Vehicle("SAME-1", VehicleType.CAR));
            } catch (VehicleAlreadyParkedException e) {
                rejected.incrementAndGet();
                return null;
            }
        });

        long accepted = 0;
        for (Future<Ticket> result : results) {
            if (result.get() != null) {
                accepted++;
            }
        }
        assertEquals(1, accepted);
        assertEquals(19, rejected.get());
        assertEquals(MEDIUM_SPOTS - 1, service.availability().get(SpotType.MEDIUM));
    }

    @Test
    void ticketIsClosedByExactlyOneConcurrentCheckout() throws Exception {
        ParkingLotService service = newService();
        Ticket ticket = service.checkIn(new Vehicle("CAR-1", VehicleType.CAR));
        AtomicInteger rejected = new AtomicInteger();

        List<Future<Boolean>> results = runTogether(20, i -> () -> {
            try {
                service.checkOut(ticket.id());
                return true;
            } catch (TicketAlreadyClosedException e) {
                rejected.incrementAndGet();
                return false;
            }
        });

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        assertEquals(1, succeeded);
        assertEquals(19, rejected.get());
        assertEquals(MEDIUM_SPOTS, service.availability().get(SpotType.MEDIUM));
    }

    @Test
    void heavyEntryAndExitTrafficLeavesLotConsistent() throws Exception {
        ParkingLotService service = newService();
        int threads = 16;
        int cycles = 500;
        AtomicInteger completed = new AtomicInteger();

        List<Future<Void>> results = runTogether(threads, t -> () -> {
            for (int c = 0; c < cycles; c++) {
                try {
                    Ticket ticket = service.checkIn(new Vehicle("T" + t + "-C" + c, VehicleType.CAR));
                    service.checkOut(ticket.id());
                    completed.incrementAndGet();
                } catch (ParkingLotFullException ignored) {
                }
            }
            return null;
        });

        for (Future<Void> result : results) {
            result.get();
        }
        Map<SpotType, Integer> availability = service.availability();
        assertEquals(MEDIUM_SPOTS, availability.get(SpotType.MEDIUM));
        assertTrue(completed.get() > 0);
    }
}
