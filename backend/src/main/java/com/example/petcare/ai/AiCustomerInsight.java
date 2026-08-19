package com.example.petcare.ai;

import java.math.BigDecimal;

public record AiCustomerInsight(
        Long customerId,
        String customerName,
        Long orderCount,
        BigDecimal paidAmount,
        BigDecimal profit,
        String lastOrderAt
) {
}
