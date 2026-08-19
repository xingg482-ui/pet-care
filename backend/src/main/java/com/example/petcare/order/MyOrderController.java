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

@RestController
@RequestMapping("/api/my/orders")
public class MyOrderController {

    private final OrderService orderService;

    public MyOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderListView>> list(
            OrderQuery query,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.listMyOrders(query, authorization));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailView> detail(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.myDetail(id, authorization));
    }

    @PostMapping
    public ApiResponse<OrderDetailView> create(
            @Valid @RequestBody OrderCreateRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.createMyOrder(request, authorization));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderDetailView> cancel(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.cancelMyOrder(id, authorization));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderPayResult> pay(
            @PathVariable Long id,
            @RequestBody(required = false) OrderPayRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(orderService.payMyOrder(id, request, authorization));
    }
}
