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
                pet.getStatus(),
                pet.getRemark(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
