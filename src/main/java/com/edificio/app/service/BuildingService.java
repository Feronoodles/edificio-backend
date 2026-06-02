package com.edificio.app.service;

import com.edificio.app.api.dto.BuildingRequest;
import com.edificio.app.api.dto.BuildingResponse;

import java.util.List;

public interface BuildingService {

    List<BuildingResponse> findAll();

    BuildingResponse findById(Long id);

    BuildingResponse create(BuildingRequest request);

    BuildingResponse update(Long id, BuildingRequest request);

    void delete(Long id);
}
