package com.edificio.app.api.dto;

import com.edificio.app.domain.Payment;
import com.edificio.app.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long apartmentId,
        String concept,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paidAt,
        PaymentStatus status,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getApartment().getId(),
                payment.getConcept(),
                payment.getAmount(),
                payment.getDueDate(),
                payment.getPaidAt(),
                payment.getStatus(),
                payment.getCreatedBy(),
                payment.getCreatedAt(),
                payment.getUpdatedBy(),
                payment.getUpdatedAt()
        );
    }
}
