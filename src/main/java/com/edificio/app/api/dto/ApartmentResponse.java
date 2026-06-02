package com.edificio.app.api.dto;

import com.edificio.app.domain.Apartment;

import java.math.BigDecimal;
import java.time.Instant;

public record ApartmentResponse(
        Long id,
        Long buildingId,
        String number,
        Integer floor,
        BigDecimal areaM2,
        boolean occupied,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static ApartmentResponse from(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getBuilding().getId(),
                apartment.getNumber(),
                apartment.getFloor(),
                apartment.getAreaM2(),
                apartment.isOccupied(),
                apartment.getCreatedBy(),
                apartment.getCreatedAt(),
                apartment.getUpdatedBy(),
                apartment.getUpdatedAt()
        );
    }
}
