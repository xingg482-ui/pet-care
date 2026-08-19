package com.example.petcare.pet;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/pets")
    public ApiResponse<PageResult<PetView>> list(PetQuery query) {
        return ApiResponse.success(petService.list(query));
    }

    @GetMapping("/pets/{id}")
    public ApiResponse<PetView> detail(@PathVariable Long id) {
        return ApiResponse.success(petService.getViewByIdOrThrow(id));
    }

    @GetMapping("/customers/{customerId}/pets")
    public ApiResponse<PageResult<PetView>> listByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "false") boolean onlyEnabled
    ) {
        return ApiResponse.success(petService.listByCustomer(customerId, onlyEnabled));
    }

    @PostMapping("/pets")
    public ApiResponse<PetView> create(@Valid @RequestBody PetRequest request) {
        return ApiResponse.success(petService.create(request));
    }

    @PutMapping("/pets/{id}")
    public ApiResponse<PetView> update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return ApiResponse.success(petService.update(id, request));
    }

    @PutMapping("/pets/{id}/status")
    public ApiResponse<PetView> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(petService.updateStatus(id, status));
    }

    @DeleteMapping("/pets/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/pets/{id}/avatar")
    public ApiResponse<PetView> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(petService.uploadAvatar(id, file));
    }

    @DeleteMapping("/pets/{id}/avatar")
    public ApiResponse<PetView> removeAvatar(@PathVariable Long id) {
        return ApiResponse.success(petService.removeAvatar(id));
    }
}
