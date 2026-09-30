package kim.autoever.taxi.ride.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RideCreateRequest(
        @NotNull(message = "출발지는 필수입니다.")
        @Valid
        LocationRequest pickup,

        @NotNull(message = "목적지는 필수입니다.")
        @Valid
        LocationRequest destination
) {
}
