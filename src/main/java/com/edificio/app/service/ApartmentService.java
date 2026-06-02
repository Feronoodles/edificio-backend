package com.edificio.app.service;

import com.edificio.app.api.dto.ApartmentRequest;
import com.edificio.app.api.dto.ApartmentResponse;

import java.util.List;

public interface ApartmentService {

    List<ApartmentResponse> findAll(Long buildingId);

    ApartmentResponse findById(Long id);

    ApartmentResponse create(ApartmentRequest request);

    ApartmentResponse update(Long id, ApartmentRequest request);

    void delete(Long id);
}
