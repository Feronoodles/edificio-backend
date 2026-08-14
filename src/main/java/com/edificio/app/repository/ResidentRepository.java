package com.edificio.app.repository;

import com.edificio.app.domain.Resident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    Page<Resident> findByDeletedFalse(Pageable pageable);

    Page<Resident> findByApartmentIdAndDeletedFalse(Long apartmentId, Pageable pageable);

    long countByApartmentIdAndDeletedFalse(Long apartmentId);

    boolean existsByApartmentIdAndActiveTrueAndDeletedFalse(Long apartmentId);
}
