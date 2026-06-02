package com.edificio.app.api.dto;

import com.edificio.app.domain.Resident;

import java.time.Instant;

public record ResidentResponse(
        Long id,
        Long apartmentId,
        String firstName,
        String lastName,
        String documentNumber,
        String email,
        String phone,
        boolean owner,
        boolean active,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static ResidentResponse from(Resident resident) {
        return new ResidentResponse(
                resident.getId(),
                resident.getApartment().getId(),
                resident.getFirstName(),
                resident.getLastName(),
                resident.getDocumentNumber(),
                resident.getEmail(),
                resident.getPhone(),
                resident.isOwner(),
                resident.isActive(),
                resident.getCreatedBy(),
                resident.getCreatedAt(),
                resident.getUpdatedBy(),
                resident.getUpdatedAt()
        );
    }
}
