package com.example.petcare.account;

public record AccountPrincipal(
        Long accountId,
        String username,
        String displayName,
        String role,
        String avatarUrl,
        Long customerId
) {
}
