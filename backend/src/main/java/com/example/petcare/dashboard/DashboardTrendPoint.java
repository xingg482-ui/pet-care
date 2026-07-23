package com.example.petcare.dashboard;

import java.math.BigDecimal;

public record DashboardTrendPoint(
        String date,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit
) {
}
