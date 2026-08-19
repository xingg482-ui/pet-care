package com.example.petcare.boarding;

import java.util.List;

public record BoardingRoomRow(
        Long locationId,
        String locationCode,
        String locationName,
        Long areaId,
        String areaName,
        String locationType,
        String petSpecies,
        String petSize,
        Integer capacity,
        String locationStatus,
        String cleanStatus,
        List<BoardingRoomCell> cells
) {
}
