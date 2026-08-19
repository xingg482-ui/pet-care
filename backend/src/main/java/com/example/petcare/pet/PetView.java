package com.example.petcare.pet;

import java.math.BigDecimal;

public record PetView(
        Long id,
        Long customerId,
        String customerName,
        String name,
        String species,
        String breed,
        String gender,
        String birthday,
        BigDecimal weight,
        Boolean sterilized,
        String color,
        String microchipNo,
        String allergies,
        String medicalHistory,
        String dietPreference,
        String behaviorNotes,
        String exerciseLevel,
        String careNotes,
        String avatarUrl,
        String avatarSource,
        String status,
        String remark,
        String createdAt,
        String updatedAt
) {
    public static PetView from(Pet pet, String customerName) {
        return new PetView(
                pet.getId(),
                pet.getCustomerId(),
                customerName,
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getBirthday(),
                pet.getWeight(),
                pet.getSterilized(),
                pet.getColor(),
                pet.getMicrochipNo(),
                pet.getAllergies(),
                pet.getMedicalHistory(),
                pet.getDietPreference(),
                pet.getBehaviorNotes(),
                pet.getExerciseLevel(),
                pet.getCareNotes(),
                pet.getAvatarUrl(),
                pet.getAvatarSource(),
                pet.getStatus(),
                pet.getRemark(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
