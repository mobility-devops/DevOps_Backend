package kim.autoever.taxi.ride.dto;

import kim.autoever.taxi.ride.domain.Location;

import java.math.BigDecimal;

public record LocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String address
) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.getLatitude(), location.getLongitude(), location.getAddress());
    }
}
