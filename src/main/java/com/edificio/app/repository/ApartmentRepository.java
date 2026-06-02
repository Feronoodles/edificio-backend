package com.edificio.app.repository;

import com.edificio.app.domain.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    List<Apartment> findByDeletedFalse();

    List<Apartment> findByBuildingIdAndDeletedFalse(Long buildingId);

    long countByBuildingIdAndDeletedFalse(Long buildingId);
}
