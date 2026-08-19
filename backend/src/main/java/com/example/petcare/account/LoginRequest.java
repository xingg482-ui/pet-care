package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String password
) {
}
