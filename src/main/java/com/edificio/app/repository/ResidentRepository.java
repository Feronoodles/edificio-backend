package com.edificio.app.repository;

import com.edificio.app.domain.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    List<Resident> findByDeletedFalse();

    List<Resident> findByApartmentIdAndDeletedFalse(Long apartmentId);

    long countByApartmentIdAndDeletedFalse(Long apartmentId);

    boolean existsByApartmentIdAndActiveTrueAndDeletedFalse(Long apartmentId);
}
