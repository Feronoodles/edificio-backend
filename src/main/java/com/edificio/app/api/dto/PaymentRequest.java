package com.edificio.app.api.dto;

import com.edificio.app.domain.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull Long apartmentId,
        @NotBlank @Size(max = 120) String concept,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotNull LocalDate dueDate,
        LocalDate paidAt,
        @NotNull PaymentStatus status
) {
}
