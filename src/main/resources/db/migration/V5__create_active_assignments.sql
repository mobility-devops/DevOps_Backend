CREATE TABLE active_assignments (
                                    id          BIGINT       NOT NULL AUTO_INCREMENT,
                                    driver_id   BIGINT       NOT NULL,
                                    ride_id     BIGINT       NOT NULL,
                                    created_at  DATETIME(6)  NOT NULL,
                                    updated_at  DATETIME(6)  NOT NULL,
                                    PRIMARY KEY (id),
                                    CONSTRAINT uk_active_assignments_driver UNIQUE (driver_id),
                                    CONSTRAINT uk_active_assignments_ride UNIQUE (ride_id),
                                    CONSTRAINT fk_active_assignments_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
                                    CONSTRAINT fk_active_assignments_ride FOREIGN KEY (ride_id) REFERENCES rides (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
