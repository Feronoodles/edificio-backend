package com.edificio.app.service.impl;

import com.edificio.app.api.dto.ApartmentRequest;
import com.edificio.app.api.dto.ApartmentResponse;
import com.edificio.app.domain.Apartment;
import com.edificio.app.exception.DeleteConflictException;
import com.edificio.app.exception.ResourceNotFoundException;
import com.edificio.app.repository.ApartmentRepository;
import com.edificio.app.repository.BuildingRepository;
import com.edificio.app.repository.PaymentRepository;
import com.edificio.app.repository.ResidentRepository;
import com.edificio.app.service.AuditService;
import com.edificio.app.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;
    private final ResidentRepository residentRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ApartmentResponse> findAll(Long buildingId, Pageable pageable) {
        var apartments = buildingId == null
                ? apartmentRepository.findByDeletedFalse(pageable)
                : apartmentRepository.findByBuildingIdAndDeletedFalse(buildingId, pageable);

        return apartments.map(ApartmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ApartmentResponse findById(Long id) {
        return ApartmentResponse.from(findApartment(id));
    }

    @Override
    @Transactional
    public ApartmentResponse create(ApartmentRequest request) {
        var apartment = new Apartment();
        apply(request, apartment);
        apartment.setCreatedBy(auditService.currentUsername());
        return ApartmentResponse.from(apartmentRepository.save(apartment));
    }

    @Override
    @Transactional
    public ApartmentResponse update(Long id, ApartmentRequest request) {
        var apartment = findApartment(id);
        apply(request, apartment);
        apartment.setUpdatedBy(auditService.currentUsername());
        apartment.setUpdatedAt(Instant.now());
        return ApartmentResponse.from(apartmentRepository.save(apartment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var apartment = findApartment(id);
        var residentCount = residentRepository.countByApartmentIdAndDeletedFalse(id);
        var paymentCount = paymentRepository.countByApartmentIdAndDeletedFalse(id);
        if (residentCount > 0 || paymentCount > 0) {
            throw new DeleteConflictException(
                    "No se puede eliminar el departamento porque tiene %d residente(s) y %d pago(s) activo(s). Elimina primero esos registros."
                            .formatted(residentCount, paymentCount)
            );
        }
        apartment.setDeleted(true);
        apartment.setDeletedBy(auditService.currentUsername());
        apartment.setDeletedAt(Instant.now());
        apartment.setNumber(deletedValue("DEL-", apartment.getId(), 20));
        apartmentRepository.save(apartment);
    }

    private Apartment findApartment(Long id) {
        return apartmentRepository.findById(id)
                .filter(apartment -> !apartment.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));
    }

    private void apply(ApartmentRequest request, Apartment apartment) {
        var building = buildingRepository.findByIdAndDeletedFalse(request.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Edificio no encontrado"));

        apartment.setBuilding(building);
        apartment.setNumber(request.number());
        apartment.setFloor(request.floor());
        apartment.setAreaM2(request.areaM2());
        apartment.setOccupied(request.occupied());
    }

    private String deletedValue(String prefix, Long id, int maxLength) {
        var value = prefix + id;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
