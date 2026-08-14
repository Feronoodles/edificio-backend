package com.edificio.app.repository;

import com.edificio.app.domain.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    Page<Apartment> findByDeletedFalse(Pageable pageable);

    Page<Apartment> findByBuildingIdAndDeletedFalse(Long buildingId, Pageable pageable);

    long countByBuildingIdAndDeletedFalse(Long buildingId);
}
