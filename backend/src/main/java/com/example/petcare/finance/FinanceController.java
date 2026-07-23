package com.example.petcare.finance;

import com.example.petcare.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/summary")
    public ApiResponse<FinanceSummary> summary() {
        return ApiResponse.success(financeService.summary());
    }

    @GetMapping("/service-items")
    public ApiResponse<List<FinanceServiceItemStat>> serviceItems(FinanceQuery query) {
        return ApiResponse.success(financeService.serviceItems(query));
    }
}
