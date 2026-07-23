package com.example.petcare.health;

import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets/{petId}/health")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @GetMapping
    public ApiResponse<HealthRecordSummary> summary(@PathVariable Long petId) {
        return ApiResponse.success(healthRecordService.summary(petId));
    }

    @PostMapping("/vaccines")
    public ApiResponse<VaccineRecord> createVaccine(@PathVariable Long petId, @Valid @RequestBody VaccineRecordRequest request) {
        return ApiResponse.success(healthRecordService.createVaccine(petId, request));
    }

    @PutMapping("/vaccines/{id}")
    public ApiResponse<VaccineRecord> updateVaccine(@PathVariable Long petId, @PathVariable Long id, @Valid @RequestBody VaccineRecordRequest request) {
        return ApiResponse.success(healthRecordService.updateVaccine(petId, id, request));
    }

    @DeleteMapping("/vaccines/{id}")
    public ApiResponse<Void> deleteVaccine(@PathVariable Long petId, @PathVariable Long id) {
        healthRecordService.deleteVaccine(petId, id);
        return ApiResponse.success();
    }

    @PostMapping("/deworming-records")
    public ApiResponse<DewormingRecord> createDeworming(@PathVariable Long petId, @Valid @RequestBody DewormingRecordRequest request) {
        return ApiResponse.success(healthRecordService.createDeworming(petId, request));
    }

    @PutMapping("/deworming-records/{id}")
    public ApiResponse<DewormingRecord> updateDeworming(@PathVariable Long petId, @PathVariable Long id, @Valid @RequestBody DewormingRecordRequest request) {
        return ApiResponse.success(healthRecordService.updateDeworming(petId, id, request));
    }

    @DeleteMapping("/deworming-records/{id}")
    public ApiResponse<Void> deleteDeworming(@PathVariable Long petId, @PathVariable Long id) {
        healthRecordService.deleteDeworming(petId, id);
        return ApiResponse.success();
    }

    @PostMapping("/weights")
    public ApiResponse<WeightRecord> createWeight(@PathVariable Long petId, @Valid @RequestBody WeightRecordRequest request) {
        return ApiResponse.success(healthRecordService.createWeight(petId, request));
    }

    @PutMapping("/weights/{id}")
    public ApiResponse<WeightRecord> updateWeight(@PathVariable Long petId, @PathVariable Long id, @Valid @RequestBody WeightRecordRequest request) {
        return ApiResponse.success(healthRecordService.updateWeight(petId, id, request));
    }

    @DeleteMapping("/weights/{id}")
    public ApiResponse<Void> deleteWeight(@PathVariable Long petId, @PathVariable Long id) {
        healthRecordService.deleteWeight(petId, id);
        return ApiResponse.success();
    }
}
