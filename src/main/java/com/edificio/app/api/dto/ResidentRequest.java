package com.edificio.app.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResidentRequest(
        @NotNull Long apartmentId,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Size(max = 30) String documentNumber,
        @Email @Size(max = 120) String email,
        @Size(max = 40) String phone,
        boolean owner,
        boolean active
) {
}
