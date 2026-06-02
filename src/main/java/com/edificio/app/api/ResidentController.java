package com.edificio.app.api;

import com.edificio.app.api.dto.ResidentRequest;
import com.edificio.app.api.dto.ResidentResponse;
import com.edificio.app.service.ResidentService;
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
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @GetMapping
    List<ResidentResponse> findAll(@RequestParam(required = false) Long apartmentId) {
        return residentService.findAll(apartmentId);
    }

    @GetMapping("/{id}")
    ResidentResponse findById(@PathVariable Long id) {
        return residentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResidentResponse create(@Valid @RequestBody ResidentRequest request) {
        return residentService.create(request);
    }

    @PutMapping("/{id}")
    ResidentResponse update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        return residentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        residentService.delete(id);
    }
}
