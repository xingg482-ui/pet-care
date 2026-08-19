package com.example.petcare.dashboard;

import java.math.BigDecimal;

public record DashboardTodayTask(
        String taskType,
        Long id,
        String taskNo,
        String customerName,
        String customerPhone,
        String petName,
        String subject,
        String taskTime,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        String targetPath
) {
}
