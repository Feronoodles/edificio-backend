package com.edificio.app.service.impl;

import com.edificio.app.api.dto.PaymentRequest;
import com.edificio.app.api.dto.PaymentResponse;
import com.edificio.app.domain.Payment;
import com.edificio.app.domain.PaymentStatus;
import com.edificio.app.exception.ResourceNotFoundException;
import com.edificio.app.repository.ApartmentRepository;
import com.edificio.app.repository.PaymentRepository;
import com.edificio.app.service.AuditService;
import com.edificio.app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApartmentRepository apartmentRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll(Long apartmentId, PaymentStatus status) {
        List<Payment> payments;
        if (apartmentId != null) {
            payments = paymentRepository.findByApartmentIdAndDeletedFalse(apartmentId);
        } else if (status != null) {
            payments = paymentRepository.findByStatusAndDeletedFalse(status);
        } else {
            payments = paymentRepository.findByDeletedFalse();
        }

        return payments.stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        return PaymentResponse.from(findPayment(id));
    }

    @Override
    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        var payment = new Payment();
        apply(request, payment);
        payment.setCreatedBy(auditService.currentUsername());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        var payment = findPayment(id);
        apply(request, payment);
        payment.setUpdatedBy(auditService.currentUsername());
        payment.setUpdatedAt(Instant.now());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var payment = findPayment(id);
        payment.setDeleted(true);
        payment.setDeletedBy(auditService.currentUsername());
        payment.setDeletedAt(Instant.now());
        paymentRepository.save(payment);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .filter(payment -> !payment.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    }

    private void apply(PaymentRequest request, Payment payment) {
        var apartment = apartmentRepository.findById(request.apartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));

        payment.setApartment(apartment);
        payment.setConcept(request.concept());
        payment.setAmount(request.amount());
        payment.setDueDate(request.dueDate());
        payment.setPaidAt(request.paidAt());
        payment.setStatus(request.status());
    }
}
