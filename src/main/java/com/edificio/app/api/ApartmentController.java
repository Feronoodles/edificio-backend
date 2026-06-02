package com.edificio.app.api;

import com.edificio.app.api.dto.ApartmentRequest;
import com.edificio.app.api.dto.ApartmentResponse;
import com.edificio.app.service.ApartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @GetMapping
    List<ApartmentResponse> findAll(@RequestParam(required = false) Long buildingId) {
        return apartmentService.findAll(buildingId);
    }

    @GetMapping("/{id}")
    ApartmentResponse findById(@PathVariable Long id) {
        return apartmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApartmentResponse create(@Valid @RequestBody ApartmentRequest request) {
        return apartmentService.create(request);
    }

    @PutMapping("/{id}")
    ApartmentResponse update(@PathVariable Long id, @Valid @RequestBody ApartmentRequest request) {
        return apartmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        apartmentService.delete(id);
    }
}
