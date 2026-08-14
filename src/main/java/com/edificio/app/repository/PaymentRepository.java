package com.edificio.app.repository;

import com.edificio.app.domain.Payment;
import com.edificio.app.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByDeletedFalse(Pageable pageable);

    Page<Payment> findByApartmentIdAndDeletedFalse(Long apartmentId, Pageable pageable);

    Page<Payment> findByStatusAndDeletedFalse(PaymentStatus status, Pageable pageable);

    Page<Payment> findByApartmentIdAndStatusAndDeletedFalse(Long apartmentId, PaymentStatus status, Pageable pageable);

    long countByApartmentIdAndDeletedFalse(Long apartmentId);
}
