package kim.autoever.taxi.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank(message = "차량 번호는 필수입니다.")
        @Size(max = 20, message = "차량 번호는 20자 이하여야 합니다.")
        String plateNumber,

        @NotBlank(message = "차종은 필수입니다.")
        @Size(max = 50, message = "차종은 50자 이하여야 합니다.")
        String model
) {
}