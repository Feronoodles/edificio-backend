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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
        payment.setPaidAmount(normalizePaidAmount(request));
        payment.setDueDate(request.dueDate());
        payment.setPaidAt(normalizePaidAt(request));
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReference(request.reference());
        payment.setStatus(request.status());
    }

    private BigDecimal normalizePaidAmount(PaymentRequest request) {
        var paidAmount = request.paidAmount() == null ? BigDecimal.ZERO : request.paidAmount();
        if (request.status() == PaymentStatus.PAID && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            paidAmount = request.amount();
        }
        if (paidAmount.compareTo(request.amount()) > 0) {
            throw new IllegalArgumentException("El monto pagado no puede ser mayor al monto del pago");
        }
        if (request.status() == PaymentStatus.PAID && paidAmount.compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Para marcar el pago como pagado, el monto pagado debe cubrir el total");
        }
        return paidAmount;
    }

    private LocalDate normalizePaidAt(PaymentRequest request) {
        if (request.status() == PaymentStatus.PAID && request.paidAt() == null) {
            return LocalDate.now();
        }
        if (request.status() != PaymentStatus.PAID) {
            return null;
        }
        return request.paidAt();
    }
}
