package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record AccountStatusRequest(
        @NotBlank(message = "不能为空") String status,
        String reason
) {
}
