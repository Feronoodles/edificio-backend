package com.edificio.app.api;

import com.edificio.app.api.dto.BuildingRequest;
import com.edificio.app.api.dto.BuildingResponse;
import com.edificio.app.service.BuildingService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    List<BuildingResponse> findAll() {
        return buildingService.findAll();
    }

    @GetMapping("/{id}")
    BuildingResponse findById(@PathVariable Long id) {
        return buildingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        return buildingService.create(request);
    }

    @PutMapping("/{id}")
    BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        return buildingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        buildingService.delete(id);
    }
}
