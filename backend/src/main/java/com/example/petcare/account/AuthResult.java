package com.example.petcare.account;

public record AuthResult(
        String token,
        Long accountId,
        String username,
        String displayName,
        String role,
        String status,
        String avatarUrl,
        Long customerId
) {
}
