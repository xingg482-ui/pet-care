package com.example.petcare.account;

import jakarta.validation.constraints.NotBlank;

public record AccountRejectRequest(
        @NotBlank(message = "不能为空") String reason
) {
}
