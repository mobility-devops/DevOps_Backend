package kim.autoever.taxi.ride.dto;

import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;

import java.time.Instant;

public record RideResponse(
        Long id,
        Long passengerId,
        Long driverId,
        RideStatus status,
        LocationResponse pickup,
        LocationResponse destination,
        Instant createdAt,
        Instant updatedAt
) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getPassenger().getId(),
                ride.getDriver() == null ? null : ride.getDriver().getId(),
                ride.getStatus(),
                LocationResponse.from(ride.getPickup()),
                LocationResponse.from(ride.getDestination()),
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }
}
