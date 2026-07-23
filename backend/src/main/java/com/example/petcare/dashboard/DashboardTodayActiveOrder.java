package com.example.petcare.dashboard;

import java.math.BigDecimal;

public record DashboardTodayActiveOrder(
        Long id,
        String orderNo,
        String customerName,
        String petName,
        String serviceNames,
        String appointmentTime,
        String status,
        BigDecimal totalAmount
) {
}
