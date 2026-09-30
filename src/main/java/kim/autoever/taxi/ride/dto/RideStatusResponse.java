package kim.autoever.taxi.ride.dto;

import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;

public record RideStatusResponse(
        Long rideId,
        RideStatus status,
        Long version
) {

    public static RideStatusResponse from(Ride ride) {
        return new RideStatusResponse(ride.getId(), ride.getStatus(), ride.getVersion());
    }
}
