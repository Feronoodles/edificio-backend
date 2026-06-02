package com.edificio.app.service.impl;

import com.edificio.app.api.dto.ResidentRequest;
import com.edificio.app.api.dto.ResidentResponse;
import com.edificio.app.domain.Resident;
import com.edificio.app.exception.ResourceNotFoundException;
import com.edificio.app.repository.ApartmentRepository;
import com.edificio.app.repository.ResidentRepository;
import com.edificio.app.service.AuditService;
import com.edificio.app.service.ResidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> findAll(Long apartmentId) {
        var residents = apartmentId == null
                ? residentRepository.findByDeletedFalse()
                : residentRepository.findByApartmentIdAndDeletedFalse(apartmentId);

        return residents.stream()
                .map(ResidentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResidentResponse findById(Long id) {
        return ResidentResponse.from(findResident(id));
    }

    @Override
    @Transactional
    public ResidentResponse create(ResidentRequest request) {
        var resident = new Resident();
        apply(request, resident);
        resident.setCreatedBy(auditService.currentUsername());
        return ResidentResponse.from(residentRepository.save(resident));
    }

    @Override
    @Transactional
    public ResidentResponse update(Long id, ResidentRequest request) {
        var resident = findResident(id);
        apply(request, resident);
        resident.setUpdatedBy(auditService.currentUsername());
        resident.setUpdatedAt(Instant.now());
        return ResidentResponse.from(residentRepository.save(resident));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var resident = findResident(id);
        resident.setDeleted(true);
        resident.setDeletedBy(auditService.currentUsername());
        resident.setDeletedAt(Instant.now());
        resident.setDocumentNumber(deletedValue("DEL-", resident.getId(), 30));
        residentRepository.save(resident);
    }

    private Resident findResident(Long id) {
        return residentRepository.findById(id)
                .filter(resident -> !resident.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Residente no encontrado"));
    }

    private void apply(ResidentRequest request, Resident resident) {
        var apartment = apartmentRepository.findById(request.apartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));

        resident.setApartment(apartment);
        resident.setFirstName(request.firstName());
        resident.setLastName(request.lastName());
        resident.setDocumentNumber(request.documentNumber());
        resident.setEmail(request.email());
        resident.setPhone(request.phone());
        resident.setOwner(request.owner());
        resident.setActive(request.active());
    }

    private String deletedValue(String prefix, Long id, int maxLength) {
        var value = prefix + id;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
