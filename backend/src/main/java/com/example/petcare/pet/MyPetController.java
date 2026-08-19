package com.example.petcare.pet;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/my/pets")
public class MyPetController {

    private final PetService petService;

    public MyPetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public ApiResponse<PageResult<PetView>> list(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(petService.listMyPets(authorization));
    }

    @PostMapping
    public ApiResponse<PetView> create(
            @Valid @RequestBody PetRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(petService.createMyPet(request, authorization));
    }

    @PutMapping("/{id}")
    public ApiResponse<PetView> update(
            @PathVariable Long id,
            @Valid @RequestBody PetRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(petService.updateMyPet(id, request, authorization));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<PetView> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(petService.updateMyPetStatus(id, status, authorization));
    }

    @PostMapping("/{id}/avatar")
    public ApiResponse<PetView> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(petService.uploadMyPetAvatar(id, file, authorization));
    }

    @DeleteMapping("/{id}/avatar")
    public ApiResponse<PetView> removeAvatar(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(petService.removeMyPetAvatar(id, authorization));
    }
}
