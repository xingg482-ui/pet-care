package com.example.petcare.support;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketMyCreateRequest(
        @NotBlank(message = "不能为空") String content
) {
}
