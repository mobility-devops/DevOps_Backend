package kim.autoever.taxi.driver.dto;

import jakarta.validation.constraints.NotNull;
import kim.autoever.taxi.driver.domain.DriverAvailability;

public record DriverAvailabilityRequest(
        @NotNull(message = "상태는 필수입니다.")
        DriverAvailability availability
) {
}