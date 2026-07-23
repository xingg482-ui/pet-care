package com.example.petcare.dashboard;

import com.example.petcare.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.success(dashboardService.summary());
    }

    @GetMapping("/revenue-trend")
    public ApiResponse<List<DashboardTrendPoint>> revenueTrend() {
        return ApiResponse.success(dashboardService.revenueTrend());
    }

    @GetMapping("/service-revenue")
    public ApiResponse<List<DashboardNameValue>> serviceRevenue() {
        return ApiResponse.success(dashboardService.serviceRevenue());
    }

    @GetMapping("/order-status")
    public ApiResponse<List<DashboardStatusStat>> orderStatus() {
        return ApiResponse.success(dashboardService.orderStatus());
    }

    @GetMapping("/today-active-orders")
    public ApiResponse<List<DashboardTodayActiveOrder>> todayActiveOrders() {
        return ApiResponse.success(dashboardService.todayActiveOrders());
    }
}
