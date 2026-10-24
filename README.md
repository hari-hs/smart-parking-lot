# Smart Parking Lot

Backend for a multi-floor parking lot, written in plain Java 17. It assigns spots by vehicle size, records entry and exit, calculates fees and updates availability on every change. State is kept in memory behind small interfaces. The database schema for a persistent version is in `docs/schema.sql`.

## Run

Requires JDK 17+ and Maven.

```bash
mvn test
mvn package
java -cp target/classes com.smartparking.App
```

## Spot allocation

| Vehicle    | Spot sizes tried, in order |
|------------|----------------------------|
| Motorcycle | small, medium, large       |
| Car        | medium, large              |
| Bus        | large                      |

`NearestFirstAllocator` keeps a sorted set of free spots for each size, ordered by floor and then spot number. It takes the first spot from the first non-empty set in the vehicle's list. This gives the closest spot of the smallest size that fits. Allocate and release are O(log n).

## Fee calculation

- Stays up to 15 minutes are free.
- Otherwise the stay is rounded up to whole hours.
- Each full 24 hours costs the daily cap. The remaining hours cost hours x hourly rate, capped at the daily cap.

| Vehicle    | Per hour | Daily cap |
|------------|----------|-----------|
| Motorcycle | 10       | 80        |
| Car        | 20       | 150       |
| Bus        | 50       | 400       |

Example: a car parked 2h 15m pays 3 x 20 = 60.

## Concurrency

- The allocator guards its free-spot sets with one `ReentrantLock`, so a spot is handed to only one vehicle.
- A concurrent set of parked license plates stops the same vehicle entering twice at once.
- `Ticket.close` is synchronized, so only one of two racing exits succeeds and the spot is released once.
- Availability listeners are notified one at a time with a fresh snapshot, so the last update always shows the final state.

## Data model

`docs/schema.sql` (MySQL 8) defines `parking_floor`, `parking_spot`, `vehicle`, `fee_rate` and `parking_ticket`. Unique indexes on generated columns of `parking_ticket` allow only one open ticket per vehicle and per spot.
