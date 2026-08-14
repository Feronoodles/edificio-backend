package com.edificio.app.api;

import com.edificio.app.api.dto.PaymentRequest;
import com.edificio.app.api.dto.PaymentResponse;
import com.edificio.app.domain.PaymentStatus;
import com.edificio.app.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    Page<PaymentResponse> findAll(
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable
    ) {
        return paymentService.findAll(apartmentId, status, pageable);
    }

    @GetMapping("/{id}")
    PaymentResponse findById(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @PostMapping
    ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        var response = paymentService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    PaymentResponse update(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        paymentService.delete(id);
    }
}
