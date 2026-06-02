package com.edificio.app.api.dto;

import com.edificio.app.domain.Building;

import java.time.LocalDateTime;

public record BuildingResponse(
        Long id,
        String name,
        String address,
        String district,
        String city,
        LocalDateTime createdAt
) {
    public static BuildingResponse from(Building building) {
        return new BuildingResponse(
                building.getId(),
                building.getName(),
                building.getAddress(),
                building.getDistrict(),
                building.getCity(),
                building.getCreatedAt()
        );
    }
}
