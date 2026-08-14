package com.edificio.app.service;

import com.edificio.app.api.dto.ResidentRequest;
import com.edificio.app.api.dto.ResidentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResidentService {

    Page<ResidentResponse> findAll(Long apartmentId, Pageable pageable);

    ResidentResponse findById(Long id);

    ResidentResponse create(ResidentRequest request);

    ResidentResponse update(Long id, ResidentRequest request);

    void delete(Long id);
}
