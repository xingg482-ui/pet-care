package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "不能为空") String oldPassword,
        @NotBlank(message = "不能为空") String newPassword
) {
}
