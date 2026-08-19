package com.example.petcare.finance;

import java.math.BigDecimal;

public record FinanceMetric(
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        BigDecimal pendingAmount,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal profitRate
) {
}
