package com.example.petcare.ai;

import com.example.petcare.finance.FinanceMetric;
import com.example.petcare.finance.FinanceServiceItemStat;

import java.util.List;

public record AiBusinessSummary(
        FinanceMetric week,
        FinanceMetric month,
        List<AiCustomerInsight> topCustomers,
        List<FinanceServiceItemStat> topServiceItems,
        List<AiStatusCount> orderStatusCounts,
        String generatedAt
) {
}
