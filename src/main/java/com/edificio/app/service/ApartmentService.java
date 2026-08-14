package com.edificio.app.service;

import com.edificio.app.api.dto.ApartmentRequest;
import com.edificio.app.api.dto.ApartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApartmentService {

    Page<ApartmentResponse> findAll(Long buildingId, Pageable pageable);

    ApartmentResponse findById(Long id);

    ApartmentResponse create(ApartmentRequest request);

    ApartmentResponse update(Long id, ApartmentRequest request);

    void delete(Long id);
}
