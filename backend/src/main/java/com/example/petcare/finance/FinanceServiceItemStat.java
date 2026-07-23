package com.example.petcare.finance;

import java.math.BigDecimal;

public record FinanceServiceItemStat(
        Long serviceItemId,
        String serviceName,
        String category,
        Long orderCount,
        Integer quantity,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal profitRate
) {
}
