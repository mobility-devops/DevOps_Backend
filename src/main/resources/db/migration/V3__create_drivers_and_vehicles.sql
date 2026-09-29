CREATE TABLE drivers (
                         id            BIGINT       NOT NULL AUTO_INCREMENT,
                         user_id       BIGINT       NOT NULL,
                         availability  VARCHAR(20)  NOT NULL,
                         created_at    DATETIME(6)  NOT NULL,
                         updated_at    DATETIME(6)  NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT uk_drivers_user UNIQUE (user_id),
                         CONSTRAINT fk_drivers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE vehicles (
                          id            BIGINT       NOT NULL AUTO_INCREMENT,
                          driver_id     BIGINT       NOT NULL,
                          plate_number  VARCHAR(20)  NOT NULL,
                          model         VARCHAR(50)  NOT NULL,
                          created_at    DATETIME(6)  NOT NULL,
                          updated_at    DATETIME(6)  NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_vehicles_driver UNIQUE (driver_id),
                          CONSTRAINT uk_vehicles_plate UNIQUE (plate_number),
                          CONSTRAINT fk_vehicles_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;