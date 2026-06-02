package com.edificio.app.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ApartmentRequest(
        @NotNull Long buildingId,
        @NotBlank @Size(max = 20) String number,
        @NotNull @Min(0) Integer floor,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal areaM2,
        boolean occupied
) {
}
