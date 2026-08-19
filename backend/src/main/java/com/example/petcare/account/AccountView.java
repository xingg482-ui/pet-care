package com.example.petcare.account;

public record AccountView(
        Long id,
        String username,
        String displayName,
        String role,
        String status,
        String phone,
        String avatarUrl,
        Long customerId,
        Long reviewedBy,
        String reviewedAt,
        String rejectReason,
        String createdAt,
        String updatedAt
) {

    public static AccountView from(Account account) {
        return new AccountView(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getStatus(),
                account.getPhone(),
                account.getAvatarUrl(),
                account.getCustomerId(),
                account.getReviewedBy(),
                account.getReviewedAt(),
                account.getRejectReason(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
