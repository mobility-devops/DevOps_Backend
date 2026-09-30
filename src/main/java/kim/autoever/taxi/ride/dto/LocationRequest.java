package kim.autoever.taxi.ride.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kim.autoever.taxi.ride.domain.Location;

import java.math.BigDecimal;

public record LocationRequest(
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        @Digits(integer = 2, fraction = 6, message = "위도는 소수점 6자리까지 입력할 수 있습니다.")
        BigDecimal latitude,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        @Digits(integer = 3, fraction = 6, message = "경도는 소수점 6자리까지 입력할 수 있습니다.")
        BigDecimal longitude,

        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address
) {

    public Location toLocation() {
        String trimmed = address == null || address.isBlank() ? null : address.trim();
        return Location.of(latitude, longitude, trimmed);
    }
}
