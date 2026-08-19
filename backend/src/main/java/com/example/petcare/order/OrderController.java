package com.example.petcare.order;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderListView>> list(OrderQuery query) {
        return ApiResponse.success(orderService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailView> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id));
    }

    @PostMapping
    public ApiResponse<OrderDetailView> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.success(orderService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderDetailView> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return ApiResponse.success(orderService.updateStatus(id, request));
    }

    @PostMapping("/{id}/payment-confirm")
    public ApiResponse<OrderPayResult> confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) OrderPayRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.confirmPayment(id, request, authorization));
    }

    @PutMapping("/{id}/appointment-time")
    public ApiResponse<OrderDetailView> updateAppointmentTime(@PathVariable Long id, @Valid @RequestBody AppointmentTimeRequest request) {
        return ApiResponse.success(orderService.updateAppointmentTime(id, request));
    }

    @GetMapping("/{id}/status-logs")
    public ApiResponse<List<OrderStatusLog>> statusLogs(@PathVariable Long id) {
        return ApiResponse.success(orderService.statusLogs(id));
    }
}
