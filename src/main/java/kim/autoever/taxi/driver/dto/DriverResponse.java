package kim.autoever.taxi.driver.dto;

import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.domain.DriverAvailability;

import java.time.Instant;

public record DriverResponse(
        Long id,
        Long userId,
        DriverAvailability availability,
        Instant createdAt,
        Instant updatedAt
) {

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getUser().getId(),
                driver.getAvailability(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}