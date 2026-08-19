package com.example.petcare.auth;

import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.example.petcare.account.AuthResult;
import com.example.petcare.account.LoginRequest;
import com.example.petcare.account.PasswordChangeRequest;
import com.example.petcare.account.RegisterRequest;
import com.example.petcare.account.RegisterResult;
import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(accountService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResult> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(accountService.register(request));
    }

    @GetMapping("/username-available")
    public ApiResponse<Boolean> usernameAvailable(@RequestParam("username") String username) {
        return ApiResponse.success(accountService.isUsernameAvailable(username));
    }

    @GetMapping("/me")
    public ApiResponse<AccountPrincipal> me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(accountService.me(authorization));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        accountService.changePassword(request, authorization);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
