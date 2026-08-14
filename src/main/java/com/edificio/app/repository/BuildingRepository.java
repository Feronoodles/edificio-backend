package com.edificio.app.repository;

import com.edificio.app.domain.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    Page<Building> findByDeletedFalse(Pageable pageable);

    Optional<Building> findByIdAndDeletedFalse(Long id);
}
