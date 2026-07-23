package com.example.petcare.finance;

import java.math.BigDecimal;

public record FinanceMetric(
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal profitRate
) {
}
