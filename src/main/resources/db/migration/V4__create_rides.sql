CREATE TABLE rides (
                       id                   BIGINT         NOT NULL AUTO_INCREMENT,
                       passenger_id         BIGINT         NOT NULL,
                       driver_id            BIGINT         NULL,
                       status               VARCHAR(20)    NOT NULL,
                       pickup_latitude      DECIMAL(9, 6)  NOT NULL,
                       pickup_longitude     DECIMAL(9, 6)  NOT NULL,
                       pickup_address       VARCHAR(255)   NULL,
                       destination_latitude  DECIMAL(9, 6) NOT NULL,
                       destination_longitude DECIMAL(9, 6) NOT NULL,
                       destination_address  VARCHAR(255)   NULL,
                       version              BIGINT         NOT NULL DEFAULT 0,
                       created_at           DATETIME(6)    NOT NULL,
                       updated_at           DATETIME(6)    NOT NULL,
                       PRIMARY KEY (id),
                       KEY idx_rides_status (status, id),
                       CONSTRAINT fk_rides_passenger FOREIGN KEY (passenger_id) REFERENCES users (id),
                       CONSTRAINT fk_rides_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE active_passenger_rides (
                                        id            BIGINT       NOT NULL AUTO_INCREMENT,
                                        passenger_id  BIGINT       NOT NULL,
                                        ride_id       BIGINT       NOT NULL,
                                        created_at    DATETIME(6)  NOT NULL,
                                        updated_at    DATETIME(6)  NOT NULL,
                                        PRIMARY KEY (id),
                                        CONSTRAINT uk_active_passenger_rides_passenger UNIQUE (passenger_id),
                                        CONSTRAINT uk_active_passenger_rides_ride UNIQUE (ride_id),
                                        CONSTRAINT fk_active_passenger_rides_passenger FOREIGN KEY (passenger_id) REFERENCES users (id),
                                        CONSTRAINT fk_active_passenger_rides_ride FOREIGN KEY (ride_id) REFERENCES rides (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
