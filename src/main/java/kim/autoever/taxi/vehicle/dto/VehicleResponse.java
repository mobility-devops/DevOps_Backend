package kim.autoever.taxi.vehicle.dto;

import kim.autoever.taxi.vehicle.domain.Vehicle;

import java.time.Instant;

public record VehicleResponse(
        Long id,
        Long driverId,
        String plateNumber,
        String model,
        Instant createdAt,
        Instant updatedAt
) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriver().getId(),
                vehicle.getPlateNumber(),
                vehicle.getModel(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}