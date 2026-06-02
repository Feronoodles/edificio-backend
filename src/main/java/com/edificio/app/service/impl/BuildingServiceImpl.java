package com.edificio.app.service.impl;

import com.edificio.app.api.dto.BuildingRequest;
import com.edificio.app.api.dto.BuildingResponse;
import com.edificio.app.domain.Building;
import com.edificio.app.exception.DeleteConflictException;
import com.edificio.app.exception.ResourceNotFoundException;
import com.edificio.app.repository.ApartmentRepository;
import com.edificio.app.repository.BuildingRepository;
import com.edificio.app.service.AuditService;
import com.edificio.app.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<BuildingResponse> findAll() {
        return buildingRepository.findByDeletedFalse().stream()
                .map(BuildingResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponse findById(Long id) {
        return BuildingResponse.from(findBuilding(id));
    }

    @Override
    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        var building = new Building();
        apply(request, building);
        return BuildingResponse.from(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public BuildingResponse update(Long id, BuildingRequest request) {
        var building = findBuilding(id);
        apply(request, building);
        return BuildingResponse.from(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var building = findBuilding(id);
        var apartmentCount = apartmentRepository.countByBuildingIdAndDeletedFalse(id);
        if (apartmentCount > 0) {
            throw new DeleteConflictException(
                    "No se puede eliminar el edificio porque tiene %d departamento(s) activo(s). Elimina primero sus residentes, pagos y departamentos."
                            .formatted(apartmentCount)
            );
        }
        building.setDeleted(true);
        building.setDeletedBy(auditService.currentUsername());
        building.setDeletedAt(LocalDateTime.now());
        buildingRepository.save(building);
    }

    private Building findBuilding(Long id) {
        return buildingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Edificio no encontrado"));
    }

    private void apply(BuildingRequest request, Building building) {
        building.setName(request.name());
        building.setAddress(request.address());
        building.setDistrict(request.district());
        building.setCity(request.city());
    }
}
