package com.edificio.app.repository;

import com.edificio.app.domain.Payment;
import com.edificio.app.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByDeletedFalse();

    List<Payment> findByApartmentIdAndDeletedFalse(Long apartmentId);

    List<Payment> findByStatusAndDeletedFalse(PaymentStatus status);

    long countByApartmentIdAndDeletedFalse(Long apartmentId);
}
