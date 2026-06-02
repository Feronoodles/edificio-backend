package com.edificio.app.service;

import com.edificio.app.api.dto.PaymentRequest;
import com.edificio.app.api.dto.PaymentResponse;
import com.edificio.app.domain.PaymentStatus;

import java.util.List;

public interface PaymentService {

    List<PaymentResponse> findAll(Long apartmentId, PaymentStatus status);

    PaymentResponse findById(Long id);

    PaymentResponse create(PaymentRequest request);

    PaymentResponse update(Long id, PaymentRequest request);

    void delete(Long id);
}
