CREATE TABLE parking_floor (
    floor_no INT PRIMARY KEY,
    label    VARCHAR(50) NULL
);

CREATE TABLE parking_spot (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    floor_no    INT NOT NULL,
    spot_number INT NOT NULL,
    spot_type   ENUM('SMALL', 'MEDIUM', 'LARGE') NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_spot_floor FOREIGN KEY (floor_no) REFERENCES parking_floor (floor_no),
    CONSTRAINT uq_spot_position UNIQUE (floor_no, spot_number),
    INDEX idx_spot_lookup (spot_type, is_occupied, floor_no, spot_number)
);

CREATE TABLE vehicle (
    license_plate VARCHAR(20) PRIMARY KEY,
    vehicle_type  ENUM('MOTORCYCLE', 'CAR', 'BUS') NOT NULL
);

CREATE TABLE fee_rate (
    vehicle_type ENUM('MOTORCYCLE', 'CAR', 'BUS') PRIMARY KEY,
    hourly_rate  DECIMAL(10, 2) NOT NULL,
    daily_cap    DECIMAL(10, 2) NOT NULL,
    grace_minutes INT NOT NULL DEFAULT 15
);

CREATE TABLE parking_ticket (
    id            CHAR(36) PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    spot_id       INT NOT NULL,
    entry_time    DATETIME(3) NOT NULL,
    exit_time     DATETIME(3) NULL,
    fee           DECIMAL(10, 2) NULL,
    active_plate  VARCHAR(20) GENERATED ALWAYS AS (IF(exit_time IS NULL, license_plate, NULL)) STORED,
    active_spot   INT GENERATED ALWAYS AS (IF(exit_time IS NULL, spot_id, NULL)) STORED,
    CONSTRAINT fk_ticket_vehicle FOREIGN KEY (license_plate) REFERENCES vehicle (license_plate),
    CONSTRAINT fk_ticket_spot FOREIGN KEY (spot_id) REFERENCES parking_spot (id),
    CONSTRAINT chk_exit_after_entry CHECK (exit_time IS NULL OR exit_time >= entry_time),
    CONSTRAINT uq_one_active_per_vehicle UNIQUE (active_plate),
    CONSTRAINT uq_one_active_per_spot UNIQUE (active_spot),
    INDEX idx_ticket_plate (license_plate, entry_time)
);

INSERT INTO fee_rate (vehicle_type, hourly_rate, daily_cap, grace_minutes) VALUES
    ('MOTORCYCLE', 10.00, 80.00, 15),
    ('CAR', 20.00, 150.00, 15),
    ('BUS', 50.00, 400.00, 15);
