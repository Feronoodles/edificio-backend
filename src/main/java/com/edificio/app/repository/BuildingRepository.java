package com.edificio.app.repository;

import com.edificio.app.domain.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findByDeletedFalse();

    Optional<Building> findByIdAndDeletedFalse(Long id);
}
