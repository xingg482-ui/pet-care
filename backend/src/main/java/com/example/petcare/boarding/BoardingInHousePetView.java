package com.example.petcare.boarding;

import java.util.List;

public record BoardingInHousePetView(
        Long boardingOrderId,
        String boardingNo,
        Long customerId,
        String customerName,
        String customerPhone,
        Long petId,
        String petName,
        String petSpecies,
        Long locationId,
        String locationCode,
        String locationName,
        Long areaId,
        String areaName,
        String actualCheckInTime,
        String plannedCheckOutTime,
        Integer stayDays,
        Integer totalTaskCount,
        Integer completedTaskCount,
        Integer pendingTaskCount,
        List<BoardingCareTaskView> tasks
) {
}
