package com.example.petcare.account;

import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<AccountView> detail(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(accountService.profile(authorization));
    }

    @PutMapping
    public ApiResponse<AccountView> update(
            @Valid @RequestBody ProfileUpdateRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.updateProfile(request, authorization));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<AccountView> restoreDefaultAvatar(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(accountService.restoreDefaultAvatar(authorization));
    }
}
