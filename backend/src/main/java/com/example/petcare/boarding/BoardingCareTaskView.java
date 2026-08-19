package com.example.petcare.boarding;

public record BoardingCareTaskView(
        Long id,
        Long boardingOrderId,
        String taskType,
        String taskName,
        String taskDate,
        String taskTime,
        String status,
        String remark,
        String completedAt
) {
}
