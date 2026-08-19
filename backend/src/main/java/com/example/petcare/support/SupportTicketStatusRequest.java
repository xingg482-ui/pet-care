package com.example.petcare.support;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketStatusRequest(
        @NotBlank(message = "不能为空") String status
) {
}
