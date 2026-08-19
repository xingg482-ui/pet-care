package com.example.petcare.support;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketCreateRequest(
        @NotBlank(message = "不能为空") String contactName,
        @NotBlank(message = "不能为空") String contactInfo,
        String username,
        @NotBlank(message = "不能为空") String issueType,
        @NotBlank(message = "不能为空") String content
) {
}
