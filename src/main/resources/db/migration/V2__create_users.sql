CREATE TABLE users (
                       id          BIGINT       NOT NULL AUTO_INCREMENT,
                       name        VARCHAR(50)  NOT NULL,
                       phone       VARCHAR(20)  NOT NULL,
                       role        VARCHAR(20)  NOT NULL,
                       created_at  DATETIME(6)  NOT NULL,
                       updated_at  DATETIME(6)  NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;