package com.example.petcare.petavatar;

public record PetAvatarLibraryView(
        Long id,
        String species,
        String breed,
        String keywords,
        String avatarUrl,
        String sourceType,
        Integer sortOrder,
        String status,
        String remark,
        String createdAt,
        String updatedAt
) {
    public static PetAvatarLibraryView from(PetAvatarLibrary avatar) {
        return new PetAvatarLibraryView(
                avatar.getId(),
                avatar.getSpecies(),
                avatar.getBreed(),
                avatar.getKeywords(),
                avatar.getAvatarUrl(),
                avatar.getSourceType(),
                avatar.getSortOrder(),
                avatar.getStatus(),
                avatar.getRemark(),
                avatar.getCreatedAt(),
                avatar.getUpdatedAt()
        );
    }
}
