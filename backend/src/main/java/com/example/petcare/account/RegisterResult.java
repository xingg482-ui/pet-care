package com.example.petcare.account;

public record RegisterResult(
        Long accountId,
        String username,
        String displayName,
        String role,
        String status,
        String message
) {
}
