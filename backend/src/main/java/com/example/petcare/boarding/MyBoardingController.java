package com.example.petcare.boarding;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import com.example.petcare.order.OrderPayRequest;
import com.example.petcare.order.OrderPayResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my/boarding")
public class MyBoardingController {

    private final BoardingService boardingService;

    public MyBoardingController(BoardingService boardingService) {
        this.boardingService = boardingService;
    }

    @GetMapping
    public ApiResponse<PageResult<BoardingOrderView>> list(
            BoardingOrderQuery query,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.listMyOrders(query, authorization));
    }

    @PostMapping
    public ApiResponse<BoardingOrderView> create(
            @Valid @RequestBody BoardingOrderRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.createMyOrder(request, authorization));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<BoardingOrderView> cancel(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.cancelMyOrder(id, authorization));
    }

    @PutMapping("/{id}/schedule")
    public ApiResponse<BoardingOrderView> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody BoardingScheduleRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.updateMySchedule(id, request, authorization));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderPayResult> pay(
            @PathVariable Long id,
            @RequestBody(required = false) OrderPayRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.payMyOrder(id, request, authorization));
    }

    @GetMapping("/care-updates")
    public ApiResponse<List<BoardingInHousePetView>> careUpdates(
            @RequestParam(required = false) String date,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.myCareUpdates(date, authorization));
    }
}
