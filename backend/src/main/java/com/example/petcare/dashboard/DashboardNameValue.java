package com.example.petcare.dashboard;

import java.math.BigDecimal;

public record DashboardNameValue(
        String name,
        BigDecimal value
) {
}
