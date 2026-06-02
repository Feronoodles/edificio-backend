package com.edificio.app.service;

import com.edificio.app.api.dto.ResidentRequest;
import com.edificio.app.api.dto.ResidentResponse;

import java.util.List;

public interface ResidentService {

    List<ResidentResponse> findAll(Long apartmentId);

    ResidentResponse findById(Long id);

    ResidentResponse create(ResidentRequest request);

    ResidentResponse update(Long id, ResidentRequest request);

    void delete(Long id);
}
