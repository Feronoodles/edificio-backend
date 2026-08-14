package com.edificio.app.service;

import com.edificio.app.api.dto.BuildingRequest;
import com.edificio.app.api.dto.BuildingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuildingService {

    Page<BuildingResponse> findAll(Pageable pageable);

    BuildingResponse findById(Long id);

    BuildingResponse create(BuildingRequest request);

    BuildingResponse update(Long id, BuildingRequest request);

    void delete(Long id);
}
