package com.example.petcare.auth;

import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String DEMO_TOKEN = "pet-care-admin-token";

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        if (!ADMIN_USERNAME.equals(request.username()) || !ADMIN_PASSWORD.equals(request.password())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return ApiResponse.success(Map.of(
                "token", DEMO_TOKEN,
                "username", ADMIN_USERNAME
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    public record LoginRequest(
            @NotBlank(message = "不能为空") String username,
            @NotBlank(message = "不能为空") String password
    ) {
    }
}
