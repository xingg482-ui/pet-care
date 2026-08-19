package com.example.petcare.petavatar;

import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PetAvatarLibraryController {

    private final PetAvatarLibraryService avatarService;

    public PetAvatarLibraryController(PetAvatarLibraryService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping("/pet-avatar-library")
    public ApiResponse<List<PetAvatarLibraryView>> list(PetAvatarLibraryQuery query) {
        return ApiResponse.success(avatarService.list(query));
    }

    @GetMapping("/pet-avatar-library/match")
    public ApiResponse<PetAvatarLibraryView> match(@RequestParam(required = false) String species,
                                                    @RequestParam(required = false) String breed) {
        return ApiResponse.success(avatarService.match(species, breed));
    }

    @PostMapping("/pet-avatar-library")
    public ApiResponse<PetAvatarLibraryView> create(@Valid @RequestBody PetAvatarLibraryRequest request) {
        return ApiResponse.success(avatarService.create(request));
    }

    @PutMapping("/pet-avatar-library/{id}")
    public ApiResponse<PetAvatarLibraryView> update(@PathVariable Long id,
                                                    @Valid @RequestBody PetAvatarLibraryRequest request) {
        return ApiResponse.success(avatarService.update(id, request));
    }

    @PutMapping("/pet-avatar-library/{id}/status")
    public ApiResponse<PetAvatarLibraryView> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(avatarService.updateStatus(id, status));
    }

    @PostMapping("/pet-avatar-library/{id}/avatar")
    public ApiResponse<PetAvatarLibraryView> uploadAvatar(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(avatarService.uploadAvatar(id, file));
    }
}
