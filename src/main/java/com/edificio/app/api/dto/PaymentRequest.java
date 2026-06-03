package com.edificio.app.api.dto;

import com.edificio.app.domain.PaymentStatus;
import com.edificio.app.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull Long apartmentId,
        @NotBlank @Size(max = 120) String concept,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @PositiveOrZero BigDecimal paidAmount,
        @NotNull LocalDate dueDate,
        LocalDate paidAt,
        PaymentMethod paymentMethod,
        @Size(max = 80) String reference,
        @NotNull PaymentStatus status
) {
}
