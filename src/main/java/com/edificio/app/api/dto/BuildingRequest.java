package com.edificio.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 200) String address,
        @Size(max = 80) String district,
        @Size(max = 80) String city
) {
}
