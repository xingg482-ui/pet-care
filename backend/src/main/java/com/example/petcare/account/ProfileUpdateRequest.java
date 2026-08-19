package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank(message = "不能为空") String displayName,
        String phone,
        String avatarUrl
) {
}
