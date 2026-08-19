package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String displayName,
        @NotBlank(message = "不能为空") String password,
        @NotBlank(message = "不能为空") String role,
        String phone,
        String avatarUrl
) {
}
